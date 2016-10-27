package angtrim.com.sampleapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import angtrim.com.fivestarslibrary.FiveStarsDialog;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final String appName = "Candy Crush";
        final String supportEmail = "info@email.com";

        new FiveStarsDialog.Builder(this)
                .withAppName(appName)
                .withSupportEmail(supportEmail)
                .withUpperBound(5) // Market opened if a rating >= 5 is selected
                .withReviewListener(new FiveStarsDialog.ReviewListener() {
                    @Override
                    public void onReview(int stars) {
                        Log.i(TAG, "User rated " + stars + " stars");
                    }
                }) // Used to listen for reviews (if you want to track them )
                .withNegativeReviewListener(new FiveStarsDialog.NegativeReviewListener() {
                    @Override
                    public void onNegativeReview(int stars) {

                        final String message = getString(R.string.rate_message);
                        final String mail = getString(R.string.rate_mail);

                        List<String> options = new ArrayList<>();
                        options.add(message);
                        options.add(mail);

                        new MaterialDialog.Builder(MainActivity.this)
                                .title(getString(R.string.rate_leave_feedback, appName))
                                //.content(R.string.rate_leave_feedback)
                                .items(options)
                                .itemsCallback(new MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                        if (message.equals(text)) {
                                            // TODO Send message
                                            Toast.makeText(MainActivity.this, "Not implemented!", Toast.LENGTH_SHORT).show();
                                        }
                                        else if (mail.equals(text)) {

                                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                            emailIntent.setType("text/email");
                                            emailIntent.putExtra(Intent.EXTRA_EMAIL,new String[] {supportEmail});
                                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " feedback");
                                            emailIntent.putExtra(Intent.EXTRA_TEXT, "");
                                            try {
                                                startActivity(Intent.createChooser(emailIntent, getString(R.string.rate_mail)));
                                            } catch (ActivityNotFoundException e) {
                                                //TODO: Handle case where no email app is available
                                                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    }
                                })
                                .positiveText(android.R.string.cancel)
                                .show();
                    }
                })
                .showAfter(1);

    }

}
