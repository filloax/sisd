package it.sisd.superslowmo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button elabora_bt = findViewById(R.id.elabora_bt);
        final Button cattura_bt = findViewById(R.id.cattura_bt);
        elabora_bt.setOnClickListener(this::buttonOnClick);
        cattura_bt.setOnClickListener(this::buttonOnClick);
    }

    public void buttonOnClick(View v){
        switch(v.getId()){
            case R.id.elabora_bt:
                showElabora();
                break;

            case R.id.cattura_bt:
                showCattura();
                break;
        }
    }

    private void showElabora(){
        Intent intent = new Intent(this, SlomoActivity.class);
        startActivity(intent);
    }

    private void showCattura(){
        Intent intent = new Intent(this, CatturaActivity.class);
        startActivity(intent);
    }
}
