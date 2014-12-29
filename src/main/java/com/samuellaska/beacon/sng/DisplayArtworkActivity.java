package com.samuellaska.beacon.sng;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Display individual art, description, author...
 *
 * Created by samuellaska on 11/28/14.
 */

public class DisplayArtworkActivity extends Activity {
    TextView title;
    TextView creator;
    ImageView image;
    TextView text;

    String strTitle;
    String strCreator;
    String SNG_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_artwork);

        // wiring to views
        title = (TextView)findViewById(R.id.title);
        creator = (TextView)findViewById(R.id.subtitle);
        image = (ImageView)findViewById(R.id.image);
        text = (TextView)findViewById(R.id.text);

        getActionBar().hide();

        // Hide the status bar.
//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);

        // extract data from intent
        Intent intent = getIntent();
        SNG_ID = intent.getStringExtra("SNG_ID");
        httpGetAsyncXML("http://www.webumenia.sk/oai-pmh/?verb=GetRecord&metadataPrefix=ese&identifier=SVK:SNG."+SNG_ID);
    }

    public void returnHome(View view) {
        onBackPressed();
    }

    public void httpGetAsyncXML(String targetURI) {

        AsyncTask<String, Integer, String> ast = new AsyncTask<String, Integer, String>() {

            ArrayList<DisplayArtworkActivity> items = null;

            // local helper method
            private String getNodeValue(Element entry, String tag) {
                Element tagElement = (Element) entry.getElementsByTagName(tag).item(0);
                return tagElement.getFirstChild().getNodeValue();
            }

            private String downloadAndParseXML(String url) {
                try {
                    InputStream in = new java.net.URL(url).openStream();

                    // build the XML parser
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    // parse and get XML elements
                    Document dom = db.parse(in);
                    Element documentElement = dom.getDocumentElement();
                    strTitle =  getNodeValue(documentElement, "dc:title");
                    strCreator = getNodeValue(documentElement, "dc:creator");
                    new DownloadImageTask(image).execute(getNodeValue(documentElement, "europeana:isShownBy"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected String doInBackground(String... urls) {
                if (urls.length <= 0)
                    return null;
                return downloadAndParseXML(urls[0]);
            }

            protected void onPostExecute(String result) {
                title.setText(strTitle);
                creator.setText(strCreator);
            }
        };
        ast.execute(targetURI);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView imageView = null;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        private Bitmap downloadImage(String url) {
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        protected Bitmap doInBackground(String... urls) {
            if (urls.length <= 0)
                return null;
            return downloadImage(urls[0]);
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null)
                this.imageView.setImageBitmap(result);
            else
                this.imageView.setImageResource(android.R.drawable.ic_delete);
        }
    }


}
