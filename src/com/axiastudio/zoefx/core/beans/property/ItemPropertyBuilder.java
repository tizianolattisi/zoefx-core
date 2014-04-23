package com.axiastudio.zoefx.core.beans.property;

import com.axiastudio.zoefx.core.beans.BeanAccess;
import javafx.beans.property.Property;
import javafx.util.Callback;

import java.util.Collection;
import java.util.Date;

/**
 * User: tiziano
 * Date: 10/04/14
 * Time: 11:08
 */
public class ItemPropertyBuilder<T> {

    private Object bean;
    private String name;
    private Class<? extends T> propertyClass;

    public ItemPropertyBuilder() {
    }

    public static ItemPropertyBuilder create(){
        return new ItemPropertyBuilder();
    }

    public static <T> ItemPropertyBuilder<T> create(Class<? extends T> klass){
        ItemPropertyBuilder<T> itemPropertyBuilder = new ItemPropertyBuilder<T>();
        itemPropertyBuilder.propertyClass = klass;
        return itemPropertyBuilder;
    }

    public ItemPropertyBuilder bean(Object bean){
        this.bean = bean;
        return this;
    }

    public ItemPropertyBuilder field(String name){
        this.name = name;
        return this;
    }

    public Property build(){
        BeanAccess beanAccess = new BeanAccess(bean, name);
        Class<?> fieldType = beanAccess.getReturnType();
        if( fieldType == null || propertyClass == null ){
            return null;
        }
        //System.out.println(bean.getClass().getSimpleName() + "." + name + " (" + fieldType + ") -> property (" + propertyClass + ")");
        if( String.class.isAssignableFrom(propertyClass) ) {
            if( String.class.isAssignableFrom(fieldType) ) {
                // String field -> String property
                ItemStringProperty<String> item = new ItemStringProperty(beanAccess);
                return item;
            } else if( Integer.class.isAssignableFrom(fieldType) ) {
                // Integer field -> String property
                ItemStringProperty<Integer> item = new ItemStringProperty(beanAccess);
                item.setToStringFunction(new Callback<Integer, String>() {
                    @Override
                    public String call(Integer i) {
                        return i.toString();
                    }
                });
                item.setFromStringFunction(new Callback<String, Integer>() {
                    @Override
                    public Integer call(String s) {
                        return Integer.parseInt(s);
                    }
                });
                return item;
            }
        } else if( Boolean.class.isAssignableFrom(propertyClass) ) {
            if( Boolean.class.isAssignableFrom(fieldType) ) {
                // Boolean field -> Boolean property
                ItemBooleanProperty<Boolean> item = new ItemBooleanProperty(beanAccess);
                return item;
            }
        } else if( Date.class.isAssignableFrom(propertyClass) ) {
            if( Date.class.isAssignableFrom(fieldType) ) {
                // Date field -> Date property
                ItemDateProperty<Date> item = new ItemDateProperty(beanAccess);
                return item;
            }
        } else if( Collection.class.isAssignableFrom(propertyClass) ) {
                // Collection field -> Collection property
                ItemListProperty item = new ItemListProperty(beanAccess);
                return item;
        } else if( Object.class.isAssignableFrom(propertyClass) ) {
            // Object field -> Object property
            ItemObjectProperty<Object> item = new ItemObjectProperty(beanAccess);
            return item;
        }
        return null;
    }

}