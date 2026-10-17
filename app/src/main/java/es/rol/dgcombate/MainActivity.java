package es.rol.dgcombate;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Delta Green Control de Combate. No usa red, cámara ni permisos de almacenamiento. */
public class MainActivity extends Activity {
    private static final int PICK_SHEET=81, PICK_MANUAL=82, SAVE_JSON=83, LOAD_JSON=84;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private WebView web;
    private LinearLayout root, readerTop, reader;
    private FrameLayout content;
    private EditText pageInput;
    private TextView pageStatus;
    private ZoomImage zoom;
    private PdfRenderer pdf;
    private ParcelFileDescriptor fd;
    private Bitmap rendered;
    private String book="A", pendingImportBook="A";
    private int currentPage=1, nextPage=1;
    private boolean isReader=false, busy=false;
    private String pendingBackup="{}";
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PDFBoxResourceLoader.init(getApplicationContext());
        getWindow().setStatusBarColor(Color.rgb(11,20,19));
        getWindow().setNavigationBarColor(Color.rgb(11,20,19));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11,20,19));
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, i)-> {
            root.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());
            return i;
        });
        readerTop = new LinearLayout(this);
        readerTop.setGravity(Gravity.CENTER_VERTICAL);
        readerTop.setBackgroundColor(Color.rgb(23,39,34));
        readerTop.addView(btn("← COMBATE",this::showWeb),new LinearLayout.LayoutParams(dp(118),dp(50)));
        pageStatus = new TextView(this);
        pageStatus.setTextColor(Color.rgb(226,224,196));
        pageStatus.setTextSize(12);
        pageStatus.setGravity(Gravity.CENTER_VERTICAL);
        pageStatus.setPadding(dp(10),0,dp(10),0);
        readerTop.addView(pageStatus,new LinearLayout.LayoutParams(0,dp(50),1));
        readerTop.addView(btn("CAMBIAR PDF",()->pickManual(book)),new LinearLayout.LayoutParams(dp(132),dp(50)));
        readerTop.setVisibility(View.GONE);
        root.addView(readerTop);
        content=new FrameLayout(this);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        web=new WebView(this);
        web.setBackgroundColor(Color.rgb(11,20,19));
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true); s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false); s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        // Fundamental en Android WebView: sin WebChromeClient los cuadros
        // prompt/confirm pueden bloquear la carga de nombres de PJ y PNJ.
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsPrompt(WebView view,String url,String message,
                                                String defaultValue,final JsPromptResult result){
                final EditText field=new EditText(MainActivity.this);
                field.setSingleLine(true);
                field.setText(defaultValue==null?"":defaultValue);
                field.setSelectAllOnFocus(true);
                field.setPadding(dp(14),dp(12),dp(14),dp(12));
                new AlertDialog.Builder(MainActivity.this)
                  .setTitle("DG Combate · Personaje")
                  .setMessage(message)
                  .setView(field)
                  .setPositiveButton("Aceptar",(d,w)->result.confirm(field.getText().toString()))
                  .setNegativeButton("Cancelar",(d,w)->result.cancel())
                  .setOnCancelListener(d->result.cancel())
                  .show();
                return true;
            }
            @Override public boolean onJsConfirm(WebView view,String url,String message,final JsResult result){
                new AlertDialog.Builder(MainActivity.this).setTitle("DG Combate")
                    .setMessage(message).setPositiveButton("Confirmar",(d,w)->result.confirm())
                    .setNegativeButton("Cancelar",(d,w)->result.cancel())
                    .setOnCancelListener(d->result.cancel()).show();
                return true;
            }
            @Override public boolean onJsAlert(WebView view,String url,String message,final JsResult result){
                new AlertDialog.Builder(MainActivity.this).setTitle("DG Combate")
                    .setMessage(message).setPositiveButton("Aceptar",(d,w)->result.confirm())
                    .setOnCancelListener(d->result.confirm()).show();
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(),"CombatBridge");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return true; }
        });
        content.addView(web,new FrameLayout.LayoutParams(-1,-1));
        reader=new LinearLayout(this); reader.setOrientation(LinearLayout.VERTICAL);
        reader.setBackgroundColor(Color.rgb(15,20,19)); reader.setVisibility(View.GONE);
        content.addView(reader,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout nav=new LinearLayout(this);
        nav.setBackgroundColor(Color.rgb(22,37,33)); nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.addView(btn("◀",()->showPage(currentPage-1)),new LinearLayout.LayoutParams(dp(65),dp(47)));
        TextView label=new TextView(this); label.setText("Pág. impresa"); label.setTextColor(Color.LTGRAY);
        label.setGravity(Gravity.CENTER); label.setTextSize(12);
        nav.addView(label,new LinearLayout.LayoutParams(dp(115),dp(47)));
        pageInput=new EditText(this); pageInput.setSingleLine(true); pageInput.setInputType(2);
        pageInput.setSelectAllOnFocus(true); pageInput.setTextColor(Color.WHITE);
        pageInput.setGravity(Gravity.CENTER); pageInput.setTextSize(15);
        nav.addView(pageInput,new LinearLayout.LayoutParams(0,dp(47),1));
        nav.addView(btn("IR",()->{
            try { showPage(Integer.parseInt(pageInput.getText().toString()) + offset(book)); }
            catch(Exception e){ toast("Introducí una página impresa válida."); }
        }),new LinearLayout.LayoutParams(dp(65),dp(47)));
        nav.addView(btn("▶",()->showPage(currentPage+1)),new LinearLayout.LayoutParams(dp(65),dp(47)));
        reader.addView(nav);
        zoom=new ZoomImage(); reader.addView(zoom,new LinearLayout.LayoutParams(-1,0,1));
        web.loadUrl("file:///android_asset/index.html");
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private Button btn(String t,Runnable r){Button b=new Button(this);b.setText(t);b.setTextSize(12);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void js(String script){ web.evaluateJavascript(script,null); }
    private void emit(String method,String message){main.post(()->js("window."+method+" && window."+method+"("+JSONObject.quote(message)+")"));}
    private boolean isKey(String k){return "A".equals(k)||"S".equals(k);}
    private int offset(String k){return "S".equals(k)?4:5;}
    private int expected(String k){return "S".equals(k)?376:202;}
    private String manualName(String k){return "S".equals(k)?"Guía del Supervisor":"Manual del Agente";}
    private File manualFile(String k){return new File(getFilesDir(),"manual_"+k+".pdf");}
    private void showWeb(){isReader=false;readerTop.setVisibility(View.GONE);reader.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);}
    private void closePdf(){if(pdf!=null){try{pdf.close();}catch(Exception ignored){}pdf=null;}if(fd!=null){try{fd.close();}catch(Exception ignored){}fd=null;}}
    private void showRule(String key,int printed){
        if(!isKey(key))return;
        book=key;
        nextPage=printed+offset(key);
        if(!manualFile(key).isFile()){toast("Importá primero el PDF de "+manualName(key));pickManual(key);return;}
        if(busy){toast("Esperá a que termine la importación.");return;}
        closePdf();showPage(nextPage);
    }
    private void showPage(int n){
        try {
            if(pdf==null){fd=ParcelFileDescriptor.open(manualFile(book),ParcelFileDescriptor.MODE_READ_ONLY);pdf=new PdfRenderer(fd);}
            if(n<1||n>pdf.getPageCount()){toast("Página fuera del manual");return;}
            currentPage=n; isReader=true;web.setVisibility(View.GONE);reader.setVisibility(View.VISIBLE);readerTop.setVisibility(View.VISIBLE);
            pageInput.setText(n>offset(book)?Integer.toString(n-offset(book)):"");
            pageInput.clearFocus();
            InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(im!=null)im.hideSoftInputFromWindow(pageInput.getWindowToken(),0);
            try(PdfRenderer.Page p=pdf.openPage(n-1)){
                int w=Math.min(2100,Math.max(900,getResources().getDisplayMetrics().widthPixels*2));
                int h=Math.min(2700,(int)((long)w*p.getHeight()/p.getWidth()));
                Bitmap fresh=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                fresh.eraseColor(Color.WHITE);p.render(fresh,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                zoom.setImageBitmap(fresh);zoom.reset(); Bitmap old=rendered;rendered=fresh;
                if(old!=null&&old!=fresh)old.recycle();
            }
            pageStatus.setText(manualName(book)+" · PDF "+n+"/"+pdf.getPageCount()+" · Impresa "+(n-offset(book)));
        } catch(Exception e){toast("No se pudo abrir el PDF. Volvé a importarlo.");closePdf();showWeb();}
    }
    private void pickManual(String key){
        if(!isKey(key))return;
        pendingImportBook=key;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");startActivityForResult(i,PICK_MANUAL);
    }
    private void pickSheet(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/png","image/jpeg","image/webp"});
        startActivityForResult(i,PICK_SHEET);
    }
    private void saveBackup(String data){
        if(data==null||data.length()>2500000){toast("El respaldo excede el máximo permitido.");return;}
        try{new JSONObject(data);}catch(Exception ex){toast("Datos JSON inválidos.");return;}
        pendingBackup=data;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"DG_Combate_Respaldo.json");
        startActivityForResult(i,SAVE_JSON);
    }
    private void loadBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,LOAD_JSON);}

    private class Bridge {
        @JavascriptInterface public void openRule(String key,int printed){main.post(()->showRule(key,printed));}
        @JavascriptInterface public void importManual(String key){main.post(()->pickManual(key));}
        @JavascriptInterface public void importSheet(){main.post(MainActivity.this::pickSheet);}
        @JavascriptInterface public void exportData(String json){main.post(()->saveBackup(json));}
        @JavascriptInterface public void importData(){main.post(MainActivity.this::loadBackup);}
        @JavascriptInterface public void saveState(String json){
            if(json!=null&&json.length()<1900000){
                try{new JSONObject(json);getPreferences(0).edit().putString("state",json).apply();}catch(Exception ignored){}
            }
        }
        @JavascriptInterface public String loadState(){return getPreferences(0).getString("state", "");}
        @JavascriptInterface public String bookStatus(){return "{"+"\"A\":"+manualFile("A").isFile()+",\"S\":"+manualFile("S").isFile()+"}";}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_MANUAL){
            if(busy)return;busy=true;String key=pendingImportBook;
            worker.execute(()->{
                File tmp=new File(getCacheDir(),"book_import_"+key+".pdf");
                try{
                    copyFile(uri,tmp,140*1024*1024);
                    int count;try(ParcelFileDescriptor pfd=ParcelFileDescriptor.open(tmp,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer test=new PdfRenderer(pfd)){count=test.getPageCount();}
                    if(count!=expected(key))throw new IOException("El manual seleccionado tiene "+count+" páginas; se esperan "+expected(key)+".");
                    File target=manualFile(key);
                    main.post(()->{try{closePdf();if(target.exists())target.delete();
                        if(!tmp.renameTo(target))throw new IOException("Error al guardar");busy=false;
                        toast("Manual importado. Disponible sin conexión.");
                        emit("dgManualUpdated",new Bridge().bookStatus());
                        if(book.equals(key))showPage(Math.max(1,Math.min(nextPage,count)));
                    }catch(Exception e){busy=false;toast("No se pudo guardar el PDF.");}});
                }catch(Exception ex){tmp.delete();main.post(()->{busy=false;toast(ex.getMessage()==null?"No se pudo importar el manual.":ex.getMessage());});}
            });
            return;
        }
        if(req==PICK_SHEET){
            if(busy)return; busy=true;toast("Leyendo ficha... Puede demorar unos segundos.");
            worker.execute(()->{
                File tmp=new File(getCacheDir(),"dg_sheet_import");
                try{
                    copyFile(uri,tmp,25*1024*1024);
                    String mime=getContentResolver().getType(uri);
                    boolean isPdf="application/pdf".equals(mime);
                    if(!isPdf){
                        try(InputStream ins=new FileInputStream(tmp)){
                            byte[] head=new byte[4];int n=ins.read(head);isPdf=n>=4&&head[0]=='%'&&head[1]=='P'&&head[2]=='D'&&head[3]=='F';
                        }
                    }
                    if(isPdf){
                        String extracted=extractPdfText(tmp);
                        if(extracted.trim().length()>70){sendSheetText(extracted,"PDF (texto)",tmp);}
                        else {scanPdfPages(tmp,0,new StringBuilder(),new JSONArray());}
                    }else{
                        Bitmap bitmap=decodeScaled(tmp,2200);
                        if(bitmap==null)throw new IOException("Imagen incompatible.");
                        recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(v->{sendSheetText(v.getText(),"Imagen (OCR)",tmp,captureLines(v,bitmap.getWidth(),bitmap.getHeight(),0));bitmap.recycle();}).addOnFailureListener(err->{bitmap.recycle();failSheet("No se pudo reconocer el texto de la imagen.",tmp);});
                    }
                }catch(Exception ex){failSheet(ex.getMessage()==null?"Error al leer la ficha.":ex.getMessage(),tmp);}
            });
            return;
        }
        if(req==SAVE_JSON){String snapshot=pendingBackup;worker.execute(()->{
            try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();out.write(snapshot.getBytes(StandardCharsets.UTF_8));main.post(()->toast("Respaldo guardado."));}
            catch(Exception e){main.post(()->toast("No se pudo guardar el respaldo."));}
        });return;}
        if(req==LOAD_JSON){worker.execute(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new IOException();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>2500000)throw new IOException("Archivo demasiado grande");}
                String raw=b.toString("UTF-8");JSONObject obj=new JSONObject(raw);
                if(!obj.has("roster")||!obj.has("version"))throw new IOException("El archivo no es un respaldo de esta aplicación.");
                getPreferences(0).edit().putString("state",raw).apply();
                emit("dgRestoreData",raw);main.post(()->toast("Respaldo restaurado."));
            }catch(Exception e){main.post(()->toast("No se pudo restaurar: "+e.getMessage()));}
        });}
    }
    private void copyFile(Uri uri,File target,long max)throws IOException{
        try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){
            if(in==null)throw new IOException("Sin acceso al archivo");byte[] b=new byte[32768];int n;long total=0;
            while((n=in.read(b))!=-1){total+=n;if(total>max)throw new IOException("Archivo demasiado grande.");out.write(b,0,n);}
        }
    }
    private String extractPdfText(File file)throws IOException {
        try(PDDocument doc=PDDocument.load(file)){
            PDFTextStripper stripper=new PDFTextStripper();stripper.setSortByPosition(true);
            stripper.setStartPage(1);stripper.setEndPage(Math.min(doc.getNumberOfPages(),5));
            StringBuilder out=new StringBuilder(stripper.getText(doc));
            PDAcroForm form=doc.getDocumentCatalog().getAcroForm();
            if(form!=null){for(PDField field:form.getFieldTree()){
                String val=field.getValueAsString();
                if(val!=null&&!val.trim().isEmpty())out.append('\n').append(field.getFullyQualifiedName()).append(": ").append(val);
            }}
            return out.length()>35000?out.substring(0,35000):out.toString();
        }
    }
    /** Retiene las coordenadas de cada línea: imprescindible para formularios con tablas. */
    private JSONArray captureLines(Text result,int width,int height,int page){
        JSONArray arr=new JSONArray();
        for(Text.TextBlock block:result.getTextBlocks()){
            for(Text.Line line:block.getLines()){
                Rect r=line.getBoundingBox(); if(r==null)continue;
                JSONObject one=new JSONObject();
                try{
                    one.put("text",line.getText());one.put("page",page);
                    one.put("x",(double)r.left/Math.max(1,width));
                    one.put("y",(double)r.top/Math.max(1,height));
                    one.put("w",(double)r.width()/Math.max(1,width));
                    one.put("h",(double)r.height()/Math.max(1,height));
                    arr.put(one);
                }catch(JSONException ignored){}
            }
        }
        return arr;
    }
    private void scanPdfPages(File file,int index,StringBuilder acc,JSONArray positions){
        if(index>=3){sendSheetText(acc.toString(),"PDF escaneado (OCR)",file,positions);return;}
        Bitmap bmp=null;
        try(ParcelFileDescriptor desc=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(desc)){
            if(index>=renderer.getPageCount()){sendSheetText(acc.toString(),"PDF escaneado (OCR)",file,positions);return;}
            try(PdfRenderer.Page p=renderer.openPage(index)){
                int width=2200;int height=(int)((long)width*p.getHeight()/p.getWidth());
                bmp=Bitmap.createBitmap(width,Math.max(1,height),Bitmap.Config.ARGB_8888);
                bmp.eraseColor(Color.WHITE);p.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            }
        }catch(Exception e){failSheet("No se pudo representar el PDF para reconocer texto.",file);return;}
        final Bitmap image=bmp;
        recognizer.process(InputImage.fromBitmap(image,0)).addOnSuccessListener(res->{
            acc.append('\n').append(res.getText());
            JSONArray here=captureLines(res,image.getWidth(),image.getHeight(),index);
            for(int i=0;i<here.length();i++)positions.put(here.opt(i));
            image.recycle();scanPdfPages(file,index+1,acc,positions);
        }).addOnFailureListener(err->{image.recycle();if(acc.length()>0)sendSheetText(acc.toString(),"PDF escaneado (OCR parcial)",file,positions);else failSheet("El reconocimiento de texto falló.",file);});
    }
    private Bitmap decodeScaled(File file,int max)throws IOException{
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getAbsolutePath(),opts);
        if(opts.outWidth<=0||opts.outHeight<=0)throw new IOException("No se pudo abrir la imagen.");
        int sample=1;while(Math.max(opts.outWidth,opts.outHeight)/sample>max)sample*=2;
        opts.inJustDecodeBounds=false;opts.inSampleSize=sample;
        return BitmapFactory.decodeFile(file.getAbsolutePath(),opts);
    }
    private void sendSheetText(String text,String source,File temp){sendSheetText(text,source,temp,new JSONArray());}
    private void sendSheetText(String text,String source,File temp,JSONArray layout){
        temp.delete();busy=false;
        if((text==null||text.trim().length()<6)&&(layout==null||layout.length()==0)){
            main.post(()->toast("No se reconoció texto. Probá con una imagen más nítida o completá los campos."));return;
        }
        try{
            JSONObject x=new JSONObject();x.put("source",source);
            x.put("text",text==null?"":(text.length()>35000?text.substring(0,35000):text));
            x.put("layout",layout==null?new JSONArray():layout);
            emit("dgImportedSheet",x.toString());
        }catch(Exception e){main.post(()->toast("Error al enviar los datos leídos."));}
    }
    private void failSheet(String msg,File f){f.delete();busy=false;main.post(()->toast(msg));}
    @Override public void onBackPressed(){if(isReader)showWeb();else new AlertDialog.Builder(this).setTitle("Salir de DG Combate")
        .setMessage("La sesión queda guardada. ¿Cerrar la aplicación?")
        .setPositiveButton("Cerrar",(d,w)->finish()).setNegativeButton("Continuar",null).show();}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdown();closePdf();
        if(rendered!=null&&!rendered.isRecycled())rendered.recycle();recognizer.close();web.destroy();}
    private class ZoomImage extends ImageView {
        private final Matrix matrix=new Matrix();private float x,y,scale=1;
        private final ScaleGestureDetector detector;
        ZoomImage(){super(MainActivity.this);setScaleType(ScaleType.MATRIX);setBackgroundColor(Color.rgb(32,36,34));
            detector=new ScaleGestureDetector(MainActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(ScaleGestureDetector d){float next=Math.min(6,Math.max(1,scale*d.getScaleFactor()));float fac=next/scale;scale=next;
                    matrix.postScale(fac,fac,d.getFocusX(),d.getFocusY());setImageMatrix(matrix);return true;}
            });
        }
        void reset(){scale=1;post(this::fit);}
        private void fit(){if(getDrawable()==null||getWidth()==0||getHeight()==0)return;
            matrix.setRectToRect(new RectF(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight()),new RectF(0,0,getWidth(),getHeight()),Matrix.ScaleToFit.CENTER);setImageMatrix(matrix);scale=1;}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);fit();}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){detector.onTouchEvent(e);
            if(e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE&&!detector.isInProgress()&&scale>1){matrix.postTranslate(e.getX()-x,e.getY()-y);setImageMatrix(matrix);}
            x=e.getX();y=e.getY();return true;}
    }
}
