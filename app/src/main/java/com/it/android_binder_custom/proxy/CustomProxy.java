package com.it.android_binder_custom.proxy;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.it.android_binder_custom.bean.Person;
import com.it.android_binder_custom.server.BinderStub;
import com.it.android_binder_custom.server.IPersonManager;

import java.util.List;

/**
 * Binder机制的发送端
 * 代理对象实质就是client最终拿到的代理服务，通过代理和Server进行通信
 */
public class CustomProxy implements IPersonManager {

    private IBinder mIBinder;

    public CustomProxy(IBinder iBinder) {
        this.mIBinder = iBinder;
    }

    /**
     * 1、首先通过Parcel将数据序列化，
     * 2、然后调用 remote.transact()将方法code，和data传输过去，
     * 对应的会回调在在Server中的onTransact()中
     * @param person
     */
    @Override
    public void addPerson(Person person) {
        Parcel data = Parcel.obtain();
        Parcel replay = Parcel.obtain();

        try {
            data.writeInterfaceToken(BinderStub.DESCRIPTOR);
            if (person != null) {
                data.writeInt(1);
                person.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            /**
             * 通过transact()方法将上述数据发送到Binder驱动
             * 参数1：目标方法标识符，（Client进程和Server进程自身约定）
             * 参数2：上述的Parcel对象
             * 参数3：结果回复
             */
            mIBinder.transact(BinderStub.ADD_PERSON, data, replay, 0);
            //在发送数据后，Client进程的该线程会暂时被挂起
            replay.readException();
        }catch (RemoteException e){
            e.printStackTrace();
        }finally {
            data.recycle();
            replay.recycle();
        }

    }


    @Override
    public List<Person> getPersonList() {
        Parcel data = Parcel.obtain();
        Parcel replay = Parcel.obtain();

        List<Person> result = null;
        try {
            data.writeInterfaceToken(BinderStub.DESCRIPTOR);
            /**
             * 通过transact()方法将上述数据发送到Binder驱动
             * 参数1：目标方法标识符，（Client进程和Server进程自身约定）
             * 参数2：上述的Parcel对象
             * 参数3：结果回复
             */
            mIBinder.transact(BinderStub.GET_PERSON, data, replay, 0);
            //在发送数据后，Client进程的该线程会暂时被挂起
            replay.readException();
            result = replay.createTypedArrayList(Person.CREATOR);
        }catch (RemoteException e){
            e.printStackTrace();
        } finally{
            replay.recycle();
            data.recycle();
        }
        return result;
    }


    // 3. Binder驱动根据代理对象 找到对应的真身Binder对象所在的Server进程（系统自动执行）
    // 4. Binder驱动把数据发送到Server进程中，并通知Server进程执行解包（系统自动执行）

    @Override
    public IBinder asBinder() {
        return null;
    }
}
