package angtrim.com.fivestarslibrary;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;


/**
 * Created by angtrim on 12/09/15.
 *
 */
public class FiveStarsDialog {

    private final static String TAG = "FiveStarsDialog";

    private final static String SP_NUM_OF_ACCESS = "pref_rating_num_access";
    private final static String SP_DISABLED = "pref_rating_disabled";

    private Context context;
    private SharedPreferences sharedPrefs;

    private RatingBar ratingBar;
    private MaterialDialog materialDialog;

    private boolean isForceMode = false;
    private int upperBound = 4;
    private String supportEmail;
    private String appName = "FiveStarsDialog";
    private int showAfter = 10;

    private ReviewListener reviewListener;
    private NegativeReviewListener negativeReviewListener;

    private FiveStarsDialog() {
    }

    private void buildDialog() {

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.stars, null);

        ratingBar = (RatingBar) dialogView.findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                Log.d(TAG, "Rating changed: " + v);
                if (isForceMode && v >= upperBound) {
                    askOpenGooglePlay();
                    disable();
                    if(reviewListener != null)
                        reviewListener.onReview((int)ratingBar.getRating());
                }
            }
        });

        materialDialog = new MaterialDialog.Builder(context)
                .title(R.string.rate_this_app_title)
                .customView(dialogView, false)
                .positiveText(R.string.rate_positive)
                .negativeText(R.string.rate_later)
                .neutralText(R.string.rate_never)
                .cancelable(false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if(ratingBar.getRating() < upperBound){
                            if(negativeReviewListener == null){
                                sendEmail();
                            }else{
                                negativeReviewListener.onNegativeReview((int)ratingBar.getRating());
                            }

                        }else if(!isForceMode){
                            askOpenGooglePlay();
                        }
                        disable();
                        if(reviewListener != null)
                            reviewListener.onReview((int)ratingBar.getRating());
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putInt(SP_NUM_OF_ACCESS, 0);
                        editor.apply();
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        disable();
                    }
                })
                .build();

    }

    private void disable() {
        SharedPreferences shared = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(SP_DISABLED, true);
        editor.apply();
    }

    private void buildAndShow() {
        boolean disabled  = sharedPrefs.getBoolean(SP_DISABLED, false);
        if(!disabled){
            buildDialog();
            materialDialog.show();
        }
    }

    private void showAfter() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int numOfAccess = sharedPrefs.getInt(SP_NUM_OF_ACCESS, 0);
        editor.putInt(SP_NUM_OF_ACCESS, numOfAccess + 1);
        editor.apply();
        if(numOfAccess + 1 >= showAfter){
            buildAndShow();
        }
    }

    public static class Builder {

        private FiveStarsDialog instance = new FiveStarsDialog();

        public Builder(Context context) {
            instance.context = context;
            instance.sharedPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        }

        public Builder withAppName(String appName) {
            instance.appName = appName;
            return this;
        }

        public Builder withSupportEmail(String supportEmail) {
            instance.supportEmail = supportEmail;
            return this;
        }

        /**
         * Set to true if you want to send the user directly to the market
         * @param isForceMode
         */
        public Builder withForceMode(boolean isForceMode){
            instance.isForceMode = isForceMode;
            return this;
        }

        /**
         * Set the upper bound for the rating.
         * If the rating is >= of the bound, the market is opened.
         * @param bound the upper bound
         */
        public Builder withUpperBound(int bound){
            instance.upperBound = bound;
            return this;
        }

        /**
         * Set a custom listener if you want to OVERRIDE the default "send email" action when the user gives a negative review
         * @param listener
         */
        public Builder withNegativeReviewListener(NegativeReviewListener listener){
            instance.negativeReviewListener = listener;
            return this;
        }

        /**
         * Set a listener to get notified when a review (positive or negative) is issued, for example for tracking purposes
         * @param listener
         */
        public Builder withReviewListener(ReviewListener listener){
            instance.reviewListener = listener;
            return this;
        }


        public Builder showAfter(int numberOfAccess) {
            instance.showAfter = numberOfAccess;
            instance.showAfter();
            return this;
        }

    }

    public interface ReviewListener {
        void onReview(int stars);
    }

    public interface NegativeReviewListener {
        void onNegativeReview(int stars);
    }

    private void askOpenGooglePlay() {

        if (materialDialog != null) {
            materialDialog.dismiss();
        }

        new MaterialDialog.Builder(context)
                .title(R.string.rate_thanks)
                .content(R.string.rate_in_gp, appName)
                .positiveText(R.string.rate_ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        rateApp(context);
                    }
                })
                .show();
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL,new String[] {supportEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        try {
            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.rate_mail)));
        } catch (ActivityNotFoundException e) {
            //TODO: Handle case where no email app is available
            Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void rateApp(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }

}
