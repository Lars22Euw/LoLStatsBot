package util.create;


import util.U;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Any {
    public static <T> T of(AnyTypeRef<T> tr) {
        if (tr.clazz == String.class) {
            return (T) alphabeticString();
        } else if (tr.clazz == Integer.class) {
            return (T) Integer.valueOf(ThreadLocalRandom.current().nextInt(100000));
        } else if (tr.clazz == Double.class) {
            return (T) Double.valueOf(ThreadLocalRandom.current().nextDouble(100));
        } else if (tr.clazz == Boolean.class) {
            return (T) Boolean.valueOf(ThreadLocalRandom.current().nextBoolean());
        } else if (tr.clazz == int.class) {
            return (T) Integer.valueOf(ThreadLocalRandom.current().nextInt(100000));
        } else if (tr.clazz == double.class) {
            return (T) Double.valueOf(ThreadLocalRandom.current().nextDouble(100));
        } else if (tr.clazz == boolean.class) {
            return (T) Boolean.valueOf(ThreadLocalRandom.current().nextBoolean());
        }
        var cons = Arrays.stream(tr.clazz.getConstructors()).
                min(Comparator.comparingInt(Constructor::getParameterCount));
        if (cons.isEmpty()) {
            return null;
        } else {
            var constructor = cons.get();
            var parameters = Arrays.stream(constructor.getParameters()).map(p -> of(findAnyRef(tr, p.getParameterizedType()))).toArray();
            try {
                var obj = constructor.newInstance(parameters);
                Arrays.stream(tr.clazz.getMethods())
                        .filter(m -> m.getName().contains("set") && m.getParameterCount() == 1
                                && m.getReturnType().equals(Void.TYPE))
                        .forEach(m -> {
                            var arg = findAnyRef(tr, m.getGenericParameterTypes()[0]);
                            try {
                                m.invoke(obj, arg);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        });
                if (obj instanceof List) {
                    U.repeat(ThreadLocalRandom.current().nextInt(2, 6), () -> ((List) obj).add(of(tr.subtypes.get(0))));
                    return (T) obj;
                } else if (obj instanceof Map) {
                    U.repeat(ThreadLocalRandom.current().nextInt(2, 6), () -> ((Map) obj).put(of(tr.subtypes.get(0)), of(tr.subtypes.get(1))));
                    return (T) obj;
                } else if (obj instanceof Set) {
                    U.repeat(ThreadLocalRandom.current().nextInt(2, 6), () -> ((Set) obj).add(of(tr.subtypes.get(0))));
                    return (T) obj;
                }
                return (T) obj;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static String alphabeticString() {
        Random random = new Random();
        return random.ints(97, 123)
            .limit(10)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }


    private static AnyTypeRef findAnyRef(AnyTypeRef<?> tr, Type type) {
        var isGenericType = typeNameLookup(tr, type.getTypeName());
        if (isGenericType != null) {
            return isGenericType;
        } else if (type instanceof ParameterizedType) {
            var parameterizedType = (ParameterizedType) type;
            var main = AnyTypeRef.of((Class<?>) parameterizedType.getRawType());
            var subtypes = Arrays.stream(parameterizedType.getActualTypeArguments()).map(ta -> findAnyRef(tr, ta)).toArray(AnyTypeRef[]::new);
            return AnyTypeRef.of(main, subtypes);
        } else {
            return AnyTypeRef.of((Class<?>) type);
        }
    }

    public static AnyTypeRef typeNameLookup(AnyTypeRef<?> tr, String typename) {
        if (typename.equals("T")) {
            return tr.subtypes.get(0);
        } else if (typename.equals("R")) {
            return tr.subtypes.get(1);
        } else if (typename.equals("S")) {
            return tr.subtypes.get(2);
        } else if (typename.equals("E")) {
            return tr.subtypes.get(0);
        } else {
            return null;
        }
    }

    public static <T> T of(Class<T> main, Class<?>... classes) {
        return of(AnyTypeRef.of(main, classes));
    }
}
