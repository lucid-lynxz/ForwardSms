package org.lynxz.forwardsms.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 修复Gson int/long 数据被解析成double的问题
 * <p>
 * 源码复制自: {@link ObjectTypeAdapter}
 * 仅修改 Number 解析部分,改为区分 double/int/long,默认实现为返回 double
 * <p>
 * 使用:
 * 1. {@link #assign2Gson(Gson)} 传入一个gson对象, 返回注入后的gson对象
 * <pre>
 *      Gson gson = MyObjectTypeAdapter.assign2Gson(new GsonBuilder()
 *                             .disableHtmlEscaping()
 *                             // .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.TRANSIENT)
 *                             // .setPrettyPrinting() // 格式化数据
 *                             .create())
 * </pre>
 */
public final class MyObjTypeAdapter extends TypeAdapter<Object> {

    /**
     * 通过反射,将自定义的 MyObjectTypeAdapter 注入到 Gson 对象中
     */
    public static Gson assign2Gson(Gson gson) {
        try {
            Field factories = Gson.class.getDeclaredField("factories");
            factories.setAccessible(true);
            Object o = factories.get(gson);
            Class<?>[] declaredClasses = Collections.class.getDeclaredClasses();
            for (Class<?> c : declaredClasses) {
                if ("java.util.Collections$UnmodifiableList".equals(c.getName())) {
                    Field listField = c.getDeclaredField("list");
                    listField.setAccessible(true);
                    List<TypeAdapterFactory> list = (List<TypeAdapterFactory>) listField.get(o);
                    int i = list.indexOf(ObjectTypeAdapter.FACTORY);
                    list.set(i, MyObjTypeAdapter.FACTORY);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gson;
    }


    // 以下代码复制自 gson 源码类: ObjectTypeAdapter ,仅修改了 NUMBER 分支的解析代码
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() == Object.class) {
                return (TypeAdapter<T>) new MyObjTypeAdapter(gson);
            }
            return null;
        }
    };

    private final Gson gson;

    MyObjTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<Object>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;
            case BEGIN_OBJECT:
                Map<String, Object> map = new LinkedTreeMap<String, Object>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;
            case STRING:
                return in.nextString();
            case NUMBER:
                // 自定义部分: 按需返回浮点/int/long
                // Gson 默认实现是返回double
                // return in.nextDouble();
                String s = in.nextString();
                if (s.contains(".")) {
                    return Double.valueOf(s);
                } else {
                    try {
                        return Integer.valueOf(s);
                    } catch (Exception e) {
                        return Long.valueOf(s);
                    }
                }
            case BOOLEAN:
                return in.nextBoolean();
            case NULL:
                in.nextNull();
                return null;
            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        TypeAdapter<Object> typeAdapter = gson.getAdapter((Class<Object>) value.getClass());
        if (typeAdapter instanceof MyObjTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }
        typeAdapter.write(out, value);
    }
}
