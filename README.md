### 仿写系统AIDL框架 学习Binder机制



目的：通过仿写AIDL加深对Android应用层和Framework层中应用的Binder机制的理解。

AIDL（Android接口定义语言）是一种框架，对Binder的封装。帮助我们方便的去使用Binder。如果直接用Binder需要写很多代码，采用AIDL可能只需要几行。就能完成跨进程通讯。

#### Binder是什么？

从IPC角度： Binder是Android中的一种跨进程通信方式。

从Android Driver层：Binder还可以理解为一种虚拟的物理设备，它的设备驱动是/dev/binder。

从Android Native层：Binder是创建Service Manager以及BpBinder/BBinder模型，搭建与binder驱动的桥梁。

从Android Framework层：Binder是各种Manager（ActivityManager、WindowManager等）和相应xxxManagerService的桥梁。

从Android 应用层：Binder是客户端和服务端进行通信的媒介，当bindService的时候，服务端会返回一个包含了服务端业务调用的 Binder对象，通过这个Binder对象，客户端就可以获取服务端提供的服务或者数据，这里的服务包括普通服务和基于AIDL的服务。



#### Binder四个重要对象

IBinder
	一个接口，代表跨进程通讯的能力，只要实现这个接口就能具备跨进程通讯的能力。
IInterface
	代表Service这一方能提供什么能力，即提供了哪些方法。
Binder
	实现了IBinder。
Stub
	使用AIDL时候，工具会给我们生成一个静态内部类Stub，这个类extends Binder 以及 implements IInterface。 这个类也是个抽象类，具体内部实现需要我们自己去实现。




#### 部分代码实现

仿写AIDL框架来模拟在Client端添加对象，Server端将对象保存到容器中，同时提供给Client端查询的方法。



**在Client通过连接Server端服务来获取到代理**

```java
/**定义接口服务*/
public interface IPersonManager extends IInterface {
    void addPerson(Person person);
    List<Person> getPersonList();
}
```

```java
//服务连接
private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        //获取到Server端提供的功能的代理接口
        iPersonManager = BinderStub.asInterface(service);
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
};

//Client端触发功能
 btnBinder.setOnClickListener(new View.OnClickListener() {
   @Override
   public void onClick(View v) {
		 //通过AIDL实现调用到Server端方法
     iPersonManager.addPerson(new Person("name" + i, i++));
     //.....省略部分代码.....
     Log.e("TAG:" + TAG,"客户端mPersonList.size()： "+iPersonManager.getPersonList().size() + "*");
	 }
});
```

```java
/**
 * 通过queryLocalInterface方法，查找本地Binder对象
 * 如果返回的就是PersonManger，说明client和server处于同一个进程，直接返回
 * 如果不是，则返回给一个代理对象
 *
 * @param iBinder Binder驱动传来的IBinder对象
 */
public static IPersonManager asInterface(IBinder iBinder) {
    if (iBinder == null) {
        return null;
    }
    IInterface iInterface = iBinder.queryLocalInterface(DESCRIPTOR);
    if (iInterface instanceof IPersonManager) {
        Log.d("TAG:","asInterface： client和server处于同一个进程，直接返回" );
        return (IPersonManager) iInterface;
    }
    Log.d("TAG:","asInterface： client和server处于不同进程，返回代理对象" );
    return new CustomProxy(iBinder);
}
```



**CustomProxy ： 作为Binder机制的发送端**

```java
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
      //....省略
    }

    // 3. Binder驱动根据代理对象 找到对应的真身Binder对象所在的Server进程（系统自动执行）
    // 4. Binder驱动把数据发送到Server进程中，并通知Server进程执行解包（系统自动执行）

    @Override
    public IBinder asBinder() {
        return null;
    }
}
```



**BinderStub： 作为Binder的接收端**

```java
public abstract class BinderStub extends Binder implements IPersonManager {

    //Binder唯一标识 自定义
    public static final String DESCRIPTOR = "com.it.binder.IPersonManager";
    // 方法标识
    public static final int GET_PERSON = IBinder.FIRST_CALL_TRANSACTION;
    public static final int ADD_PERSON = IBinder.FIRST_CALL_TRANSACTION + 1;

  	//......省略部分代码.....

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply,
                                 int flags) throws RemoteException {
        // 1. 收到Binder驱动通知后，Server 进程通过回调Binder对象onTransact（）进行数据解包 & 调用目标方法
        // code即在transact（）中约定的目标方法的标识符
        switch (code) {
            case INTERFACE_TRANSACTION:
                return true;

            case GET_PERSON:
                data.enforceInterface(DESCRIPTOR);
                List<Person> personList = this.getPersonList();
                if (reply != null) {
                    reply.writeNoException();
                    reply.writeTypedList(personList);
                }
                return true;

            case ADD_PERSON:
                // 解包Parcel中的数据
                data.enforceInterface(DESCRIPTOR);
                Person person = null;
                // 解析目标方法对象的标识符
                if (data.readInt() != 0) {
                    person = Person.CREATOR.createFromParcel(data);
                }
                this.addPerson(person);
                // 将结果写入到reply
                if (reply != null)
                    reply.writeNoException();
                return true;
        }

        // 2. 将结算结果返回 到Binder驱动
        return super.onTransact(code, data, reply, flags);
    }
}
```



**mStub对象，也就是Server中的Binder实体对象，在这里做Server端操作。**

```java
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
```