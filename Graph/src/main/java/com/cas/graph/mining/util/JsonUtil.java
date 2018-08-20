package com.cas.graph.mining.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.topsec.tsm.base.type.IpAddress;
import com.topsec.tsm.util.DateUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

public class JsonUtil {


    public static Object parse(JSONObject rowJson, String xpath) {
        String[] arr = xpath.split("\\.");

        Object val = null;
        for (int i = 0; i < arr.length; i++) {
            String key = arr[i].split("#")[0];
            val = rowJson.get(key);
            if (val instanceof JSONObject) {
                rowJson = (JSONObject) val;
            } else if (val instanceof JSONArray) {
            }
        }
        return val;
    }

    /**
     * @param data       json字符串(json数组)
     * @param subFormats json模版
     * @return list <k -v>
     */
    public static List<Map<String, Object>> parseJSONArray(JSONArray data, List<JsonFormat> subFormats) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (data != null) {
            Iterator<Object> subit = data.iterator();
            while (subit.hasNext()) {
                JSONObject subdata = (JSONObject) subit.next();
                Map<String, Object> subrow = parseJSONObject(subdata, subFormats);
//                subrow.put("MESSAGE",subdata.toString());
//                subrow.put("INPUTTIME",new Date());
                rows.add(subrow);
            }
        }
        return rows;
    }

    /**
     * @param data        json对象
     * @param jsonFormats json模版
     * @return 返回 k - v
     */
    public static Map<String, Object> parseJSONObject(JSONObject data, List<JsonFormat> jsonFormats) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("MESSAGE", data.toString());
        row.put("INPUTTIME", new Date());

        try {
            for (JsonFormat flowFormat : jsonFormats) {
                if (flowFormat.getJsonUrl().startsWith("$"))
                    continue;
                String key = flowFormat.getColumnName();
                Object value = JsonUtil.parseCol(data, flowFormat.getJsonUrl(), null);
                if (null == value || "".equals(value))
                    continue;
                if (flowFormat.getDatatype().equals("Date")) {
                    try {
                        Date startTime = DateUtils.fromStringToDate(value.toString(), "yyyy-MM-dd hh:mm:ss");
                        value = startTime;
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (flowFormat.getDatatype().equals("")) {
                    IpAddress ipAddress = new IpAddress(value.toString());
                    value = ipAddress;
                }
                row.put(key, value);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            row.put("TOPSEC_SOURCE", data.toString());
            return row;
        }
        return row;
    }

    /**
     * @param json
     * @param xpath
     * @param node
     * @return
     */
    public static Object parseCol(Object json, String xpath, String node) {
        if (xpath == null)
            return json;
        if (xpath.contains(".")) {
            node = xpath.substring(0, xpath.indexOf("."));
            xpath = xpath.substring(xpath.indexOf(".") + 1, xpath.length());
        } else {
            node = xpath;
            xpath = null;
        }

        if (json instanceof JSONObject) {
            return parseCol_JSONObject(json, node, xpath);
        } else if (json instanceof JSONArray) {
            return parseCol_JSONArray(json, node);
        } else {
            return json;
        }
    }

    private static Object parseCol_JSONObject(Object json, String node, String xpath) {
        JSONObject jsonObject = (JSONObject) json;
        if (node.contains("|")) {
            return parseCol_JSONObject_0(jsonObject, node, xpath);
        } else if (node.contains("#")) {
            return parseCol_JSONObject_1(jsonObject, node, xpath);
        } else {
            Object object = jsonObject.get(node);
            return parseCol(object, xpath, node);
        }
    }

    private static Object parseCol_JSONObject_0(JSONObject jsonObject, String node, String xpath) {
        String[] arr = node.split("\\|");
        List<Object> objects = new ArrayList<Object>();
        for (String a : arr) {
            Object object = jsonObject.get(a);
            Object o = parseCol(object, xpath, node);
            objects.add(o);
        }
        return objects;
    }

    private static Object parseCol_JSONObject_1(JSONObject jsonObject, String node, String xpath) {
        String[] arr = node.split("#");
        Object object = jsonObject.get(arr[0]);
        if (null == object)
            return null;
        if (node.endsWith("#"))
            return object.toString();
        if ((arr.length == 2) && (object instanceof JSONArray)) {
            JSONArray arrjson = (JSONArray) object;
            if (arrjson.size() > 0) {
                Object o = arrjson.get(Integer.parseInt(arr[1]));
                return parseCol(o, xpath, node);
            } else {
                return null;
            }
        }
        return object;
    }

    private static Object parseCol_JSONArray(Object json, String node) {
        JSONArray arrjson = (JSONArray) json;
        if (arrjson.size() == 0)
            return null;
        if (node.contains("#")) {
            return parseCol_JSONArray_1(arrjson, node);
        } else {
            List<Object> objects = new ArrayList<Object>();
            for (int i = 0; arrjson != null && i < arrjson.size(); i++) {
                JSONObject jsonObject = arrjson.getJSONObject(i);
                Object object = parseCol(jsonObject, node, null);
                objects.add(object);
            }
            return objects;
        }
    }

    private static Object parseCol_JSONArray_1(JSONArray arrjson, String node) {
        String[] arr = node.split("#");
        List<Object> objects = new ArrayList<Object>();

        for (int i = 0; arrjson != null && i < arrjson.size(); i++) {
            JSONObject jsonObject = arrjson.getJSONObject(i);
            Object object = parseCol(jsonObject, arr[0], null);
            objects.add(object);
        }
        if (arr.length == 2) {
            return objects.get(Integer.parseInt(arr[1]));
        }
        return objects;
    }


    public static String object2json(Object obj) {
        StringBuilder json = new StringBuilder();
        if (obj == null) {
            json.append("\"\"");
        } else if (obj instanceof String || obj instanceof Integer || obj instanceof Float
                || obj instanceof Boolean || obj instanceof Short || obj instanceof Double
                || obj instanceof Long || obj instanceof BigDecimal
                || obj instanceof BigInteger || obj instanceof Byte) {
            json.append("\"").append(string2json(obj.toString())).append("\"");
        } else if (obj instanceof Object[]) {
            json.append(array2json((Object[]) obj));
        } else if (obj instanceof List) {
            json.append(list2json((List<?>) obj));
        } else if (obj instanceof Map) {
            json.append(map2json((Map<?, ?>) obj));
        } else if (obj instanceof Set) {
            json.append(set2json((Set<?>) obj));
        } else if (obj instanceof Date) {
            try {
                json.append("\"" + DateUtils.fromDate2String((Date) obj, "yyyy-MM-dd hh:mm:ss") + "\"");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (obj instanceof IpAddress) {
            json.append("\"" + ((IpAddress) obj).toString() + "\"");
        } else {
            json.append(bean2json(obj));
        }
        return json.toString();
    }

    /**
     * @param bean bean对象
     * @return String
     */
    public static String bean2json(Object bean) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        PropertyDescriptor[] props = null;
        try {
            props = Introspector.getBeanInfo
                    (bean.getClass(), Object.class).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        if (props != null) {
            for (int i = 0; i < props.length; i++) {
                try {
                    String name = object2json(props[i].getName());
                    String value = object2json(props[i].getReadMethod().invoke(bean));
                    json.append(name);
                    json.append(":");
                    json.append(value);
                    json.append(",");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            json.setCharAt(json.length() - 1, '}');
        } else {
            json.append("}");
        }
        return json.toString();
    }

    /**
     * @param list list对象
     * @return String
     */
    public static String list2json(List<?> list) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        if (list != null && list.size() > 0) {
            for (Object obj : list) {
                json.append(object2json(obj));
                json.append(",");
            }
            json.setCharAt(json.length() - 1, ']');
        } else {
            json.append("]");
        }
        return json.toString();
    }

    /**
     * @param array 对象数组
     * @return String
     */
    public static String array2json(Object[] array) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        if (array != null && array.length > 0) {
            for (Object obj : array) {
                json.append(object2json(obj));
                json.append(",");
            }
            json.setCharAt(json.length() - 1, ']');
        } else {
            json.append("]");
        }
        return json.toString();
    }

    /**
     * @param map map对象
     * @return String
     */
    public static String map2json(Map<?, ?> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        if (map != null && map.size() > 0) {
            for (Object key : map.keySet()) {
                json.append(object2json(key));
                json.append(":");
                json.append(object2json(map.get(key)));
                json.append(",");
            }
            json.setCharAt(json.length() - 1, '}');
        } else {
            json.append("}");
        }
        return json.toString();
    }

    /**
     * @param set 集合对象
     * @return String
     */
    public static String set2json(Set<?> set) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        if (set != null && set.size() > 0) {
            for (Object obj : set) {
                json.append(object2json(obj));
                json.append(",");
            }
            json.setCharAt(json.length() - 1, ']');
        } else {
            json.append("]");
        }
        return json.toString();
    }

    /**
     * @param s 参数
     * @return String
     */
    public static String string2json(String s) {
        if (null == s) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if (ch >= '\u0000' && ch <= '\u001F') {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sb.append('0');
                        }
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

}