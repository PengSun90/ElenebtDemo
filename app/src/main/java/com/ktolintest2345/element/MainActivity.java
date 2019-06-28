package com.ktolintest2345.element;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.ktolintest2345.apt_api.BindView;

public class MainActivity extends AppCompatActivity {

  @BindView(R.id.tv_content)
  TextView tvContent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.inject(this);

    tvContent.setText("这就是ButterKnife的原理");
  }
}
