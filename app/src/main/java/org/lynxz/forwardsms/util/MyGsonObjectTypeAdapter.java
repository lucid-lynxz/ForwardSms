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
 * version: 1.0
 * date: 2020.9.27
 * 参考: [彻底解决 Gson 将 int 转换为 double 的问题](https://blog.csdn.net/qq_17457105/article/details/89282563)
 * <p>
 * 自定义 GSON object数据解析类, 重写 NUMBER 的解析部分, 修复 int/long 反序列化为 double 的问题
 * 实现代码直接拷贝自 {@link com.google.gson.internal.bind.ObjectTypeAdapter}
 * <p>
 * 使用:
 * 通过 {@link #assign2Gson(Gson)} 来注入自定义适配器,并返回 Gson 对象
 */
public final class MyGsonObjectTypeAdapter extends TypeAdapter<Object> {

    /**
     * 通过反射, 将自定义的 ObjectTypeAdapter 注入到 Gson 对象中,然后返回 Gson
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
                    int i = list == null ? -1 : list.indexOf(ObjectTypeAdapter.FACTORY);
                    if (i >= 0) {
                        list.set(i, MyGsonObjectTypeAdapter.FACTORY);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return gson;
    }

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() == Object.class) {
                return (TypeAdapter<T>) new MyGsonObjectTypeAdapter(gson);
            }
            return null;
        }
    };

    private final Gson gson;

    MyGsonObjectTypeAdapter(Gson gson) {
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
                // 自定义修改 NUMBER 数据的解析,按需返回 double/int/long 类型1
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
        if (typeAdapter instanceof MyGsonObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }

        typeAdapter.write(out, value);
    }
}