package com.it.android_binder_custom.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.it.android_binder_custom.bean.Person;
import com.it.android_binder_custom.server.BinderStub;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * onBind方法返回mStub对象，也就是Server中的Binder实体对象
 */
public class ServerService extends Service {

    public static final String TAG = ServerService.class.getSimpleName();
    private List<Person> mPersonList = new ArrayList<>();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }

   private IBinder mStub =  new BinderStub() {
        @Override
        public void addPerson(Person person) {
            if (person == null) {
                Log.e("TAG:"+TAG, "null obj");
                person = new Person();
            }

            mPersonList.add(person);
            Log.e("TAG:"+TAG, "服务端mPersonList.size()： "+mPersonList.size());
        }

        @Override
        public List<Person> getPersonList() {
            return mPersonList;
        }
    };

}
