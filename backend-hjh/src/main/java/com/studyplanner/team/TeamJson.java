package com.studyplanner.team;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
@Component public class TeamJson {
 public final ObjectMapper mapper;
 public TeamJson(ObjectMapper mapper){this.mapper=mapper;}
 public String write(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("数据无法序列化");}}
 public <T>T read(String value,Class<T> type){try{return mapper.readValue(value,type);}catch(Exception e){throw new IllegalArgumentException("保存的数据无法解析");}}
}
