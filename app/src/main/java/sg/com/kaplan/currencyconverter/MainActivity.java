package sg.com.kaplan.currencyconverter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //define members that correspond to Views in our layout

    private Button mCalButton;
    private TextView mConvertedAmt;
    private EditText mAmount;
    private Spinner mOrgSpinner, mFinalSpinner;
    private String[] mCurrencies;

    public static final String ORG = "ORG_CURRENCY";
    public static final String FINAL = "FINAL_CURRENCY";
    //this will contain my developers key
    private String mKey;
    //used to fetch the 'rates' jason object from openexchangerates.org
    public static final String RATES = "rates";
    public static final String URL_BASE = "http://openexchangerates.org/api/latest.json?app_id=";
    //used to format data from openexchangerates.org
    private static final DecimalFormat DECIMAL_FORMAT = new
            DecimalFormat("#,##0.00000");






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //add icon to actionbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.ic_launcher);


        // unpack ArrayList from the bundle and convert to array
        ArrayList<String> arrayList = ((ArrayList<String>)
                getIntent().getSerializableExtra(SplashActivity.KEY_ARRAYLIST));
        Collections.sort(arrayList);
        mCurrencies = arrayList.toArray(new String[arrayList.size()]);

    //assign references to our Views
        mConvertedAmt = (TextView) findViewById(R.id.converted);
        mAmount = (EditText) findViewById(R.id.edt_amount);
        mCalButton = (Button) findViewById(R.id.calculate);
        mOrgSpinner = (Spinner) findViewById(R.id.spn_origin);
        mFinalSpinner = (Spinner) findViewById(R.id.spn_final);

        //controller:  model and view
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String> (

                //context
                this,
                //view: layout you see when the spinner is closed
                R.layout.spinner_closed,
                //model: the array of Strings
                mCurrencies
        );

        //view: layout you see when the spinner is open
        arrayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        //assign adapters to spinners
        mOrgSpinner.setAdapter(arrayAdapter);
        mFinalSpinner.setAdapter(arrayAdapter);

        mOrgSpinner.setOnItemSelectedListener(this);
        mFinalSpinner.setOnItemSelectedListener(this);


        //set to shared-preferences or pull from shared-preferences on restart
        if (savedInstanceState == null
                && (PrefsMgr.getString(this, ORG) == null &&
        PrefsMgr.getString(this, FINAL) == null)) {
            mOrgSpinner.setSelection(findPositionGivenCode("VND", mCurrencies));
            mFinalSpinner.setSelection(findPositionGivenCode("USD", mCurrencies));
            PrefsMgr.setString(this, ORG, "VND");
            PrefsMgr.setString(this, FINAL,"USD");


        } else {
            mOrgSpinner.setSelection(findPositionGivenCode(PrefsMgr.getString(this,ORG), mCurrencies));
            mFinalSpinner.setSelection(findPositionGivenCode(PrefsMgr.getString(this,FINAL),mCurrencies));
        }
        mCalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNumeric(String.valueOf(mAmount.getText()))){
                    new CurrencyConverterTask().execute(URL_BASE + mKey);
                } else {
                    Toast.makeText(MainActivity.this, "Not a numeric value, try again.", Toast.LENGTH_LONG).show();
                }

            }
        });
        mKey = getKey("open_key");

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }
    private void launchBrowser(String strUri) {
        if (isOnline()) {
            Uri uri = Uri.parse(strUri);
            //call an implicit intent
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
    private void invertCurrencies() {
        int nFor = mOrgSpinner.getSelectedItemPosition();
        int nHom = mFinalSpinner.getSelectedItemPosition();
        mOrgSpinner.setSelection(nHom);
        mFinalSpinner.setSelection(nFor);
        mConvertedAmt.setText("");

        PrefsMgr.setString(this, ORG, extractCodeFromCurrency((String)
                mOrgSpinner.getSelectedItem()));
        PrefsMgr.setString(this, FINAL, extractCodeFromCurrency((String)
                mFinalSpinner.getSelectedItem()));
    }
    private int findPositionGivenCode(String code, String[] currencies) {

        for (int i = 0; i < currencies.length; i++) {
            if (extractCodeFromCurrency(currencies[i]).equalsIgnoreCase(code)) {
                return i;
            }
        }
        //default
        return 0;
    }

    private String extractCodeFromCurrency(String currency) {
        return (currency).substring(0,3);
    }

    private String getKey(String keyName){
        AssetManager assetManager = this.getResources().getAssets();
        Properties properties = new Properties();
        try {
            InputStream inputStream = assetManager.open("keys.properties");
            properties.load(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  properties.getProperty(keyName);

    }

    public static boolean isNumeric(String str) {
        try{
            double dub = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.inverted:
                invertCurrencies();
                break;
            case R.id.menu:
                launchBrowser(SplashActivity.URL_CODES);
                break;
            case R.id.exit_app:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId()) {

            case R.id.spn_origin:
                PrefsMgr.setString(this, ORG,
                        extractCodeFromCurrency((String)mOrgSpinner.getSelectedItem()));
                break;

            case R.id.spn_final:
                PrefsMgr.setString(this, FINAL,
                        extractCodeFromCurrency((String)mFinalSpinner.getSelectedItem()));
                break;

            default:
                break;
        }

        mConvertedAmt.setText("");

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class CurrencyConverterTask extends AsyncTask<String, Void, JSONObject> {
        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Calculating Result...");
            progressDialog.setMessage("One moment please...");
            progressDialog.setCancelable(true);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    "Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CurrencyConverterTask.this.cancel(true);
                            progressDialog.dismiss();
                        }
                    });
            progressDialog.show();
        }
        @Override
        protected JSONObject doInBackground(String... params) {
            return new JSONParser().getJSONFromUrl(params[0]);
        }
        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            double dCalculated = 0.0;
            String strForCode =
                    extractCodeFromCurrency(mCurrencies[mOrgSpinner.getSelectedItemPosition()]);
            String strHomCode = extractCodeFromCurrency(mCurrencies[mFinalSpinner.
                    getSelectedItemPosition()]);
            String strAmount = mAmount.getText().toString();
            try {
                if (jsonObject == null){
                    throw new JSONException("no data available.");
                }
                JSONObject jsonRates = jsonObject.getJSONObject(RATES);
                if (strHomCode.equalsIgnoreCase("USD")){
                    dCalculated = Double.parseDouble(strAmount) / jsonRates.getDouble(strForCode);
                } else if (strForCode.equalsIgnoreCase("USD")) {
                    dCalculated = Double.parseDouble(strAmount) * jsonRates.getDouble(strHomCode) ;
                }
                else {
                    dCalculated = Double.parseDouble(strAmount) * jsonRates.getDouble(strHomCode)
                            / jsonRates.getDouble(strForCode) ;
                }
            } catch (JSONException e) {
                Toast.makeText(
                        MainActivity.this,
                        "There's been a JSON exception: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                mConvertedAmt.setText("");
                e.printStackTrace();
            }
            mConvertedAmt.setText(DECIMAL_FORMAT.format(dCalculated) + " " + strHomCode);
            progressDialog.dismiss();

        }
    }
}





