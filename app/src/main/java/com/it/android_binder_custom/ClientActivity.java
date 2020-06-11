package com.it.android_binder_custom;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.it.android_binder_custom.bean.Person;
import com.it.android_binder_custom.server.BinderStub;
import com.it.android_binder_custom.server.IPersonManager;
import com.it.android_binder_custom.service.ServerService;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class ClientActivity extends AppCompatActivity {

    public static final String TAG = ClientActivity.class.getSimpleName();
    private Button btnBinder;
    private TextView tvContent;
    private boolean isConnect = false;
    private IPersonManager iPersonManager;
    private  int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        btnBinder = (Button) findViewById(R.id.btn_binder);
        tvContent = (TextView) findViewById(R.id.tv_content);

        //初始化连接服务端
        initBindService();

        btnBinder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iPersonManager == null) {
                    Log.e("TAG:" + TAG, "connect error");
                    return;
                }

                iPersonManager.addPerson(new Person("name" + i, i++));

                List<Person> personList = iPersonManager.getPersonList();
                StringBuilder builder = new StringBuilder();
                for (Person person : personList) {
                    builder.append(person.toString()).append("\n");
                }
                tvContent.setText(builder.toString());

                Log.e("TAG:" + TAG,
                        "客户端mPersonList.size()： "+iPersonManager.getPersonList().size() + "*");
            }
        });
    }

    private void initBindService() {
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }



    //服务连接
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("TAG:" + TAG, "connect success");
            Toast.makeText(getApplicationContext(),"connect success", Toast.LENGTH_SHORT).show();
            isConnect = true;
            //Server端提供的功能的代理接口
            iPersonManager = BinderStub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("TAG:" + TAG, "connect failed");
            Toast.makeText(getApplicationContext(),"connect failed", Toast.LENGTH_SHORT).show();
            isConnect = false;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnect) {
            unbindService(connection);
            connection = null;
        }
    }
}
