package org.lynxz.version.util;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义gson 对象解析适配器,修复 int/long 数据被解析成double的问题
 * <p>
 * 代码复制自 Gson 源码类: {@link ObjectTypeAdapter}
 * 仅修改了 Number 类型的解析部分, 默认返回 double, 改为具体区分 double/int/long
 * <p>
 * 使用:
 * {@link #assign2Gson(Gson)} 传入一个gson对象, 返回注入后的gson对象
 */
public final class MyObjectTypeAdapter extends TypeAdapter<Object> {

    /**
     * 通过反射,将自定义的 MyObjectTypeAdapter 注入到 Gson 对象中
     */
    public static Gson assign2Gson(Gson gson) {
//        try {
//            Field factories = Gson.class.getDeclaredField("factories");
//            factories.setAccessible(true);
//            Object o = factories.get(gson);
//            Class<?>[] declaredClasses = Collections.class.getDeclaredClasses();
//            for (Class<?> c : declaredClasses) {
//                if ("java.util.Collections$UnmodifiableList".equals(c.getName())) {
//                    Field listField = c.getDeclaredField("list");
//                    listField.setAccessible(true);
//                    List<TypeAdapterFactory> list = (List<TypeAdapterFactory>) listField.get(o);
//                    int i = list.indexOf(ObjectTypeAdapter.FACTORY);
//                    list.set(i, MyObjectTypeAdapter.FACTORY);
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return gson;
    }


    // 以下代码复制自 gson 源码类: ObjectTypeAdapter ,仅修改了 NUMBER 分支的解析代码
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() == Object.class) {
                return (TypeAdapter<T>) new MyObjectTypeAdapter(gson);
            }
            return null;
        }
    };

    private final Gson gson;

    MyObjectTypeAdapter(Gson gson) {
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

        TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) gson.getAdapter(value.getClass());
        if (typeAdapter instanceof MyObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }
        typeAdapter.write(out, value);
    }
}
