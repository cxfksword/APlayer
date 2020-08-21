package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import remix.myplayer.R;
import remix.myplayer.ui.activity.base.BaseActivity;

public class WelcomeActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actvity_welcome);

        startActivity(new Intent(this, PlayerActivity.class));
        finish();
    }
}
