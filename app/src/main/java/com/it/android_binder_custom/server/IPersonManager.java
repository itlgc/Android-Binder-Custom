package com.it.android_binder_custom.server;

import android.os.IInterface;

import com.it.android_binder_custom.bean.Person;

import java.util.List;

/**
 * 定义接口服务
 * 服务端具备功能提供给客户端，定义一个接口继承IInterface
 */
public interface IPersonManager extends IInterface {

    void addPerson(Person person);

    List<Person> getPersonList();
}
