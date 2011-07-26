// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.wireless.speed.speedometer.util;

import com.google.myjson.FieldNamingPolicy;
import com.google.myjson.Gson;
import com.google.myjson.GsonBuilder;
import com.google.myjson.JsonDeserializationContext;
import com.google.myjson.JsonDeserializer;
import com.google.myjson.JsonElement;
import com.google.myjson.JsonParseException;
import com.google.myjson.JsonPrimitive;
import com.google.myjson.JsonSerializationContext;
import com.google.myjson.JsonSerializer;
import com.google.wireless.speed.speedometer.MeasurementDesc;
import com.google.wireless.speed.speedometer.MeasurementTask;
import com.google.wireless.speed.speedometer.SpeedometerApp;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class that use the gson library to provide bidirectional conversion between
 * measurement objects (descriptions, tasks, and results, etc.) and JSON objects. 
 * New types of MeasurementDesc should be registered in the static HashMap initialization
 * section.
 * @author wenjiezeng@google.com (Steve Zeng)
 *
 */
@SuppressWarnings("rawtypes")
public class MeasurementJsonConvertor {
  /* 1. Automatically perform bidirectional translations for fields in java 'lowerCaseCamel' style 
   * to JSON 'lower_case_with_underscores' style. 
   * 2. Serialize and de-serialize UTC format date string
   * 3. It also serializes all null fields to 'null'
   */
  public static Gson gson = new GsonBuilder().serializeNulls().
      registerTypeAdapter(Date.class, new DateTypeConverter()).
      setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  private static final DateFormat dateFormat = 
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  
  @SuppressWarnings("unchecked")
  public static MeasurementTask makeMeasurementTaskFromJson(JSONObject json, Context context) 
      throws IllegalArgumentException {  
    try {
      String type = String.valueOf(json.getString("type"));   
      Class taskClass = MeasurementTask.getTaskClassForMeasurement(type);
      Method getDescMethod = taskClass.getMethod("getDescClass");
      // The getDescClassForMeasurement() is static and takes no arguments
      Class descClass = (Class) getDescMethod.invoke(null, (Object[]) null);
      MeasurementDesc measurementDesc = gson.fromJson(json.toString(), descClass);
      Object[] cstParams = {measurementDesc, context};
      Constructor<MeasurementTask> constructor = 
          taskClass.getConstructor(MeasurementDesc.class, Context.class);
      return constructor.newInstance(cstParams);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    } catch (SecurityException e) {
      Log.w(SpeedometerApp.TAG, e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (NoSuchMethodException e) {
      Log.w(SpeedometerApp.TAG, e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (IllegalArgumentException e) {
      Log.w(SpeedometerApp.TAG, e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (InstantiationException e) {
      Log.w(SpeedometerApp.TAG, e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (IllegalAccessException e) {
      Log.w(SpeedometerApp.TAG, e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      Log.w(SpeedometerApp.TAG, e.toString());
      throw new IllegalArgumentException(e);
    } 
  }
  
  public static JSONObject encodeToJson(Object obj) throws JSONException {
    String str = gson.toJson(obj);
    return new JSONObject(str);
  }
  
  public static String toJsonString(Object obj) {
    return gson.toJson(obj);
  }

  private static class DateTypeConverter implements JsonSerializer<Date>,
                                                        JsonDeserializer<Date> {
    @Override
    public JsonElement serialize(Date src, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(formatDate(src));
    }

    @Override
    public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
      try {
        return parseDate(json.getAsString());
      } catch (IllegalArgumentException e) {
        Log.e(SpeedometerApp.TAG, "Cannot convert UTC time string:" + json.toString());
        throw new JsonParseException("Cannot convert UTC time string: " + json.toString());
      } catch (ParseException e) {
        throw new JsonParseException("Cannot convert UTC time string: "  + json.toString());
      }
    }
  }
  
  public static Date parseDate(String dateString) throws ParseException {
    return dateFormat.parse(dateString);
  }

  private static String formatDate(Date date) {
    return dateFormat.format(date);
  }

}
