package com.wyc.common.service;
import com.wyc.common.annotation.*;
import com.wyc.common.util.CommonUtil;
import com.wyc.common.util.JdbcUtil;
import lombok.Data;
import org.springframework.data.domain.*;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

abstract public class BaseAbstractService<T>{
    @Data
    private class ConditionVo{
        private String column;
        private PartType type;
        private Object value;
        private String fun;
        private List<String> funParams;
        private String open;
        private String end;
    }


    @Data
    private class SelectPropertyVo{
        private String column;
        private String name;
        private String asName;
        private String fun;
    }

    @Data
    private class TableVo{
        private String table;
        private List<SelectPropertyVo> selectPropertys;
        private List<ConditionVo> conditions;
    }

    private Class<T> persistentClass;

    abstract  protected DataSource dataSource();
    abstract  protected  String database();

    public BaseAbstractService(){this.persistentClass = (Class<T>) getSuperClassGenricType(getClass(),0);}

    private String toString(Object value){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if(CommonUtil.isNotEmpty(value)){
            if(value.getClass().equals(Timestamp.class)){
                Timestamp timestamp = (Timestamp)value;
                return sf.format(new Date(timestamp.getTime()));
            }else if(value.getClass().equals(Date.class)){
                Date date = (Date)value;
                return sf.format(date);
            }else{
                return value.toString();
            }
        }else{
            return null;
        }
    }

    public void update(T t)throws Exception{
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Class<?> type = t.getClass();
        Field[] fields = type.getDeclaredFields();
        Field updateAtField = null;
        StringBuffer sb = new StringBuffer();

        Table table = type.getAnnotation(Table.class);
        String tableName = table.name();

        String idName = "";
        Object idValue = "";

        sb.append("update "+tableName+" set ");

        List<Object> params = new ArrayList<>();
        for(Field field:fields){
            UpdateAt updateAt = field.getAnnotation(UpdateAt.class);
            if(CommonUtil.isNotEmpty(updateAt)){
                updateAtField = field;
            }

            field.setAccessible(true);

            if(CommonUtil.isNotEmpty(updateAtField)){
                updateAtField.set(t,new Timestamp(new Date().getTime()));
            }

            Id id = field.getAnnotation(Id.class);
            Column column = field.getAnnotation(Column.class);

            if(field.get(t)==null){
                continue;
            }

            if(CommonUtil.isNotEmpty(id)){
                idName = column.name();
                idValue = field.get(t);
            }else if(CommonUtil.isNotEmpty(column)){
                sb.append(column.name()+"=?");
                sb.append(",");
                params.add(field.get(t));
            }
        }

        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append(" where "+idName+"='"+idValue+"'");

        JdbcUtil.executeQuery(dataSource(),sb.toString(),params);
    }


    public <P> P findOne(Class<P> type,String sql){
        List<P> all = findAll(type,sql);
        if(all.size()==1){
            return all.get(0);
        }else if(all.size()>1){
            throw new RuntimeException("有多条记录");
        }else {
            return null;
        }
    }

    public T findOne(String id){
        try{
            String idName = "";
            for(Field field:persistentClass.getDeclaredFields()){
                Id idAnn = field.getAnnotation(Id.class);
                if(CommonUtil.isNotEmpty(idAnn)){
                    Column column = field.getAnnotation(Column.class);
                    idName = column.name();
                }
            }

            TableVo tableVo = new TableVo();
            tableVo.setSelectPropertys(selectPropertys(this.persistentClass));
            Table table = this.persistentClass.getAnnotation(Table.class);
            String name = table.name();
            tableVo.setTable(name);

            List<ConditionVo> conditionVos = new ArrayList<>();
            ConditionVo conditionVo = new ConditionVo();
            conditionVo.setColumn(idName);
            conditionVo.setType(PartType.SIMPLE_PROPERTY);
            conditionVo.setValue(id);
            conditionVos.add(conditionVo);
            tableVo.setConditions(conditionVos);

            Pageable pageable = new PageRequest(0,1);
            Page<T> all = findAll(tableVo,pageable);

            if(CommonUtil.isNotEmpty(all)){
                if(all.getContent().size()==1){
                    return all.getContent().get(0);
                }else if(all.getContent().size()>1){
                    throw new RuntimeException("数量超过一条记录");
                }else {
                    return null;
                }
            }else{
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("执行findOne方法发生错误",e);
        }
    }

    public void delete(String id){
        if(CommonUtil.isEmpty(id)){
            throw new RuntimeException("id不能为空");
        }
        StringBuffer sb = new StringBuffer();
        Table table = this.persistentClass.getAnnotation(Table.class);
        String idName = "";
        for(Field field:this.persistentClass.getDeclaredFields()){
            Id idAnn = field.getAnnotation(Id.class);
        }
    }

    public <P> List<P> findAll(Class<P> type,String sql){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            List<P> targets = new ArrayList<>();
            List<Map<String,Object>> list = JdbcUtil.executeQuery(dataSource(),sql);
            for(Map<String,Object> entry:list){
                P target = type.newInstance();
                for(Map.Entry<String,Object> map:entry.entrySet()){
                    try{
                        Field field = null;
                        for(Field thisField:type.getDeclaredFields()){
                            Column column = thisField.getAnnotation(Column.class);
                            if(column.name().equals(map.getKey())){
                                field = thisField;
                                break;
                            }
                        }
                        setValue(field,target,map.getValue());
                    }catch (Exception e2){
                        e2.printStackTrace();
                        throw new RuntimeException("執行findAll方法發生錯誤",e2);
                    }
                }
                targets.add(target);
            }
            return targets;
        }catch (Exception e){
            throw new RuntimeException("执行findAll方法发成错误",e);
        }
    }

    private void setValue(Field field,Object target,Object value)throws Exception{
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        field.setAccessible(true);
        if(field.getType().equals(Integer.class)||field.getType().equals(int.class)){
            try{
                field.set(target,Integer.valueOf(value.toString()));

            }catch (Exception e){
                field.set(target,0);
            }
        }else if(field.getType().equals(Long.class) || field.getType().equals(long.class)){
            try{
                field.set(target,Long.valueOf(value.toString()));
            }catch (Exception e){
                field.set(target,0L);
            }
        }else if(field.getType().equals(Float.class)|| field.getType().equals(float.class)){
            try {
                field.set(target, Float.valueOf(value.toString()));
            }catch (Exception e){
                field.set(target,0f);
            }
        }else if(field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)){
            try{
                field.set(target,Boolean.valueOf(value.toString()));
            }catch (Exception e){
                field.set(target,false);
            }
        }else if(field.getType().equals(String.class)){
            if(CommonUtil.isEmpty(value)){
                field.set(target,null);
            }else{
                field.set(target,value.toString());
            }
        }else if(field.getType().equals(Double.class)|| field.getType().equals(double.class)){
            try{
                field.set(target,Double.valueOf(value.toString()));
            }catch (Exception e){
                field.set(target,0d);
            }
        }else if(field.getType().equals(Timestamp.class)){
            if(CommonUtil.isNotEmpty(value)){
                try {
                    try {
                        Date date = sf.parse(value.toString());
                        Timestamp t1 = new Timestamp(date.getTime());
                        field.set(target, t1);
                    } catch (Exception e) {
                        SimpleDateFormat sf2 = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = sf2.parse(value.toString());
                        Timestamp t1 = new Timestamp(date.getTime());
                        field.set(target, t1);
                    }
                }catch (Exception e){
                    long lt = new Long(value.toString());
                    Timestamp t1 = new Timestamp(lt);
                    field.set(target,t1);
                }
            }else{
                field.set(target,null);
            }
        }else if(field.getType().equals(Date.class)){
            if(CommonUtil.isNotEmpty(value)){
                try {
                    try {
                        Date date = sf.parse(value.toString());

                        field.set(target, date);
                    } catch (Exception e) {
                        SimpleDateFormat sf2 = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = sf2.parse(value.toString());
                        field.set(target, date);
                    }
                }catch (Exception e){
                    long lt = new Long(value.toString());
                    Timestamp t1 = new Timestamp(lt);
                    field.set(target,new Date(t1.getTime()));
                }
            }else{
                field.set(target,null);
            }
        }
        else if(field.getType().equals(BigDecimal.class)){
            if(CommonUtil.isNotEmpty(value)){
                field.set(target,new BigDecimal(value.toString()));
            }else{
                field.set(target,new BigDecimal("0"));
            }
        }else{
            throw new RuntimeException("类型不在此范围，请针对该类型增加一种判断");
        }
    }

    public void add(T t){
        StringBuffer sb = new StringBuffer();
        try{
            Class<?> type = t.getClass();
            Field[] fields = type.getDeclaredFields();
            Field idField = null;

            Field createAtField = null;

            Field updateAtField = null;

            Map<String,Object> entites = new HashMap<>();

            for(Field field:fields){
                Id id = field.getAnnotation(Id.class);
                if(CommonUtil.isNotEmpty(id)){
                    idField = field;
                }

                CreateAt createAt = field.getAnnotation(CreateAt.class);

                if(CommonUtil.isNotEmpty(createAt)){
                    createAtField = field;
                }

                UpdateAt updateAt = field.getAnnotation(UpdateAt.class);

                if(CommonUtil.isNotEmpty(updateAt)){
                    updateAtField = field;
                }

                field.setAccessible(true);

                if(CommonUtil.isNotEmpty(createAtField)){
                    createAtField.setAccessible(true);
                    createAtField.set(t,new Timestamp(new Date().getTime()));
                }

                if(CommonUtil.isNotEmpty(updateAtField)){
                    updateAtField.setAccessible(true);
                    updateAtField.set(t,new Timestamp(new Date().getTime()));
                }

                Column column = field.getAnnotation(Column.class);

                if(CommonUtil.isNotEmpty(column)){
                    if(CommonUtil.isNotEmpty(id)){
                        Object idValue = field.get(t);
                        boolean auto = id.auto();
                        if(auto&&CommonUtil.isNotEmpty(idValue)){
                            entites.put(column.name(),idValue);
                        }else{
                            Id.Generator generator = id.generator();
                            if(generator.equals(Id.Generator.NATIVE)){

                            }else if(generator.equals(Id.Generator.SEQUENCE)){
                                String sequence = id.sequence();
                                idValue = getValue("select "+sequence+".nextval from dual");
                                entites.put(column.name(),idValue);

                                idField.setAccessible(true);
                                if(CommonUtil.isNotEmpty(idValue)){
                                    setValue(field,t,idValue);
                                }
                            }else if(generator.equals(Id.Generator.UUID)){
                                String uuid = UUID.randomUUID().toString();
                                idField.setAccessible(true);
                                idField.set(t,uuid);
                                entites.put(column.name(),uuid);
                                setValue(field,t,uuid);
                            }
                        }
                    }else{
                        if(CommonUtil.isNotEmpty(field.get(t))){
                            entites.put(column.name(),field.get(t));
                        }
                    }
                }
            }

            Table table = type.getAnnotation(Table.class);

            List<Object> values = new ArrayList<>();
            sb.append("insert into "+table.name());
            sb.append("(");

            for(Map.Entry<String,Object> entry:entites.entrySet()){
                sb.append("`"+entry.getKey()+"`");
                sb.append(",");
                values.add(entry.getValue());
            }

            sb.deleteCharAt(sb.lastIndexOf(","));
            sb.append(")");
            sb.append("values(");

            for(Map.Entry<String,Object> entry:entites.entrySet()){
                sb.append("?");
                sb.append(",");
            }

            sb.deleteCharAt(sb.lastIndexOf(","));
            sb.append(")");

            JdbcUtil.executeQuery(dataSource(),sb.toString(),values);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Object getValue(String sql){
        try{
            List<Map<String,Object>> list = JdbcUtil.executeQuery(dataSource(),sql);
            if(list.size()>0){
                Map<String,Object> map = list.get(0);

                for(Map.Entry<String,Object> entry:map.entrySet()){
                    return entry.getValue();
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        return null;
    }

    public T findMax(String name)throws Exception{
        Field[] fields = this.persistentClass.getDeclaredFields();
        Field maxField = null;
        String column = null;
        for(Field field:fields){
            if(field.getName().equals(name)){
                maxField = field;
                Column fieldColumn = field.getAnnotation(Column.class);
                if(CommonUtil.isNotEmpty(fieldColumn)){
                    column = fieldColumn.name();
                }

                if(CommonUtil.isEmpty(column)){
                    column = field.getName().toLowerCase();
                }
                break;
            }
        }

        List<SelectPropertyVo> selectPropertys = selectPropertys(this.persistentClass);
        TableVo tableVo = table(this.persistentClass.newInstance());

        StringBuffer sb = new StringBuffer();

        sb.append("select ");

        for(SelectPropertyVo selectProperty:tableVo.getSelectPropertys()){
            sb.append(selectProperty.column);
            sb.append(",");
        }

        sb.deleteCharAt(sb.lastIndexOf(","));

        sb.append(" from ");
        sb.append(tableVo.getTable());

        sb.append(" where ");
        sb.append(column+" = ");
        sb.append("(select max("+column+") from "+tableVo.getTable()+")");

        List<Map<String,Object>> list = JdbcUtil.executeQuery(dataSource(),sb.toString());

        List<T> results = new ArrayList<>();

        for(Map<String,Object> record:list){
            T target = this.persistentClass.newInstance();

            for(SelectPropertyVo selectProperty:selectPropertys){
                String name1 = selectProperty.getName();
                Field field = this.persistentClass.getDeclaredField(name1);
                field.setAccessible(true);
                field.set(target,record.get(selectProperty.getColumn()));
            }

            results.add(target);
        }

        if(results.size()>0){
            return results.get(0);
        }else{
            return null;
        }
    }

    public T findOne(T t){
        try{
            List<T> all = findAll(t);
            if(CommonUtil.isEmpty(all)||all.size()==0){
                return null;
            }

            if(all.size()>1){
                throw new RuntimeException("findOne 返回了对跳数据记录");
            }
            return all.get(0);
        }catch (Exception e){
            throw e;
        }
    }

    public List<T> findAll(T t){
        return findAll(t,null).getContent();
    }

    public String getPaginationSql(String sql,long startRow,long endRow){
        if(startRow<0||endRow<startRow){
            throw new IllegalArgumentException("OracleDialect.getPaginationSql has been passed an illegal or inappropriate argument" +
                    "(startRow < 0 || endRow <startRow)");
        }

        StringBuffer sqln = new StringBuffer(
                "select * from (select row_.*,rownum rownum_ from(");

        sqln.append(trim(sql)).append(")row_ where rownum<=").append(endRow).append(" ) where rownum_>= ").append(startRow);

        return sqln.toString();
    }

    private String trim(String sql){
        String sqlt = sql.trim();
        if(sqlt.equals(";")){
            sqlt = sqlt.substring(0,sql.length()-1 - ";".length());
        }
        return sqlt;
    }

    private Page<T> findAll(TableVo tableVo,Pageable pageable)throws Exception{
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuffer sb = new StringBuffer();

        sb.append("select ");

        for(SelectPropertyVo selectProperty:tableVo.getSelectPropertys()){
            String fun = selectProperty.getFun();
            if(CommonUtil.isEmpty(fun)){
                sb.append("`"+selectProperty.column+"`");
            }else{
                sb.append(fun+"("+selectProperty.column+")");
            }

            sb.append(" as "+selectProperty.getAsName());
            sb.append(",");
        }

        sb.append("1 ");
        sb.append(" from ");
        sb.append(tableVo.getTable());
        sb.append(" where ");
        sb.append(" 1 = 1 ");
        if(tableVo.getConditions().size()>0){
            sb.append(" and ");
        }
        int index = 0;
        for(ConditionVo conditionVo:tableVo.getConditions()){
            index++;
            sb.append(conditionSql(conditionVo)+" ");
            if(index<tableVo.getConditions().size()){
                sb.append(" and ");
            }
        }

        if(CommonUtil.isNotEmpty(pageable)){
            int pageNumber = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            long offset = pageable.getOffset();
            Sort sort = pageable.getSort();

            if(CommonUtil.isNotEmpty(sort)){
                Iterator<Sort.Order> orderIterator = sort.iterator();
                if(orderIterator.hasNext()){
                    sb.append(" order by ");
                }

                while (orderIterator.hasNext()){
                    Sort.Order order = orderIterator.next();
                    sb.append(order.getProperty());
                    sb.append(" ");
                    sb.append(order.getDirection().toString());
                    sb.append(" ");
                }
            }

            if (database().equals("mysql")) {
                sb.append(" limit ");
                sb.append(offset);
                sb.append(",");
                sb.append(offset+(pageNumber+1)*pageSize);
            }else if(database().equals("oracle")){
                String str = getPaginationSql(sb.toString(),offset,offset+((pageNumber+1)*pageSize));
                sb = new StringBuffer();
                sb.append(str);
            }
        }

        List<Map<String,Object>> list = JdbcUtil.executeQuery(dataSource(),sb.toString());
        List<SelectPropertyVo> selectPropertys = tableVo.getSelectPropertys();

        List<T> results = new ArrayList<>();
        for(Map<String,Object> record:list){
            T target = this.persistentClass.newInstance();
            for(SelectPropertyVo selectProperty:selectPropertys){
                String name = selectProperty.getName();
                Field field = this.persistentClass.getDeclaredField(name);
                field.setAccessible(true);
                String asName = selectProperty.getAsName();
                if(database().equals("oracle")){
                    asName = asName.toUpperCase();
                }
                Object value = record.get(asName);
                setValue(field,target,value);
            }
            results.add(target);
        }

        StringBuffer countBuffer = new StringBuffer();

        countBuffer.append("select count(*)");
        countBuffer.append(" from ");
        countBuffer.append(tableVo.getTable());
        countBuffer.append(" where ");
        countBuffer.append(" 1=1 ");
        if(tableVo.getConditions().size()>0){
            countBuffer.append(" and ");
        }

        int countIndex = 0;
        for(ConditionVo condtionVo:tableVo.getConditions()){
            countIndex++;
            countBuffer.append(conditionSql(condtionVo)+" ");
            if(countIndex< tableVo.getConditions().size()){
                countBuffer.append("and ");
            }
        }

        Object count = getValue(countBuffer.toString());
        PageImpl page = new PageImpl(results,pageable,Long.valueOf(count.toString()));
        return page;
    }

    public Page<T> findAll(T t,Pageable pageable){
        if(CommonUtil.isEmpty(pageable)){
            pageable = new PageRequest(0,10000);
        }
        try{
            TableVo tableVo = table(t);
            return findAll(tableVo,pageable);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public String conditionSql(ConditionVo condition){
        String fun = condition.getFun();
        List<String> funParams = condition.getFunParams();
        PartType type = condition.getType();
        if(CommonUtil.isEmpty(type) || type.equals(PartType.SIMPLE_PROPERTY)){
            if (CommonUtil.isEmpty(fun)) {
                String sql = condition.getColumn()+" ='"+ toString(condition.getValue())+"'";
                return sql;
            }else{
                StringBuffer sb = new StringBuffer();
                sb.append(condition.getColumn()+"=");
                sb.append(fun);
                sb.append("(");
                sb.append(toString(condition.getValue()));
                if(CommonUtil.isNotEmpty(funParams)){
                    for(String param:funParams){
                        sb.append(",");
                        sb.append(param);
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.IN)){
            List<Object> list = (List<Object>) condition.getValue();
            StringBuffer sb = new StringBuffer();
            sb.append(condition.getColumn()+" in");
            sb.append("(");

            for(Object obj:list){
                if(CommonUtil.isEmpty(fun)){
                    sb.append("'");
                    sb.append(toString(obj));
                    sb.append("'");
                }else{
                    sb.append(fun);
                    sb.append("(");
                    sb.append("'");
                    sb.append(toString(obj));
                    sb.append("'");

                    if(CommonUtil.isNotEmpty(funParams)){
                        for(String param:funParams){
                            sb.append(",");
                            sb.append(param);
                        }
                    }
                    sb.append(")");
                }
                sb.append(",");

            }

            if(list.size()>0){
                sb.deleteCharAt(sb.lastIndexOf(","));
            }
            sb.append(")");
            return sb.toString();
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.GREATER_THAN_EQUAL)){
            if(CommonUtil.isEmpty(fun)){
                String sql = condition.getColumn()+">='"+toString(condition.getValue())+"'";
                return sql;
            }else{
                StringBuffer sb = new StringBuffer();
                sb.append(condition.getColumn()+">=");
                sb.append(fun);
                sb.append("('");
                if(CommonUtil.isNotEmpty(funParams)){
                    for(String param:funParams){
                        sb.append(",");
                        sb.append("'");
                        sb.append(param);
                        sb.append("'");
                    }
                }

                sb.append(")");
                return sb.toString();
            }
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.LESS_THAH_EQUAL)){
            if(CommonUtil.isEmpty(fun)){
                String sql = condition.getColumn()+"<='"+toString(condition.getValue())+"'";
                return sql;
            }else{
                StringBuffer sb = new StringBuffer();
                sb.append(condition.getColumn()+"<=");
                sb.append(fun);
                sb.append("('");
                sb.append(toString(condition.getValue()));
                sb.append("'");
                if(CommonUtil.isNotEmpty(funParams)){
                    for(String param:funParams){
                        sb.append(",");
                        sb.append("'");
                        sb.append(param);
                        sb.append("'");
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.GREATER_THAN)){
            if(CommonUtil.isEmpty(fun)){
                String sql = condition.getColumn()+">'"+toString(condition.getValue())+"'";
                return sql;
            }else{
                StringBuffer sb = new StringBuffer();
                sb.append(condition.getColumn()+">=");
                sb.append(fun);
                sb.append("('");
                sb.append(toString(condition.getValue()));
                sb.append("'");
                if(CommonUtil.isNotEmpty(funParams)){
                    for(String param:funParams){
                        sb.append(",");
                        sb.append("'");
                        sb.append(param);
                        sb.append("'");
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.LESS_THAN)){
            if(CommonUtil.isEmpty(fun)){
                String sql = condition.getColumn()+"<"+toString(condition.getValue())+"'";
                return sql;
            }else{
                StringBuffer sb = new StringBuffer();
                sb.append(condition.getColumn()+"<");
                sb.append(fun);
                sb.append("('");
                sb.append(toString(condition.getValue()));
                sb.append("'");
                if(CommonUtil.isNotEmpty(funParams)){
                    for(String param:funParams){
                        sb.append(",");
                        sb.append("'");
                        sb.append(param);
                        sb.append("'");
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        }else if(CommonUtil.isNotEmpty(type)&&type.equals(PartType.IS_NULL)){
            StringBuffer sb = new StringBuffer();
            sb.append(condition.getColumn()+" is null ");
            return sb.toString();
        }else{
            throw new RuntimeException("类型不在范围");
        }

    }

    private TableVo table(T t)throws Exception{
        TableVo tableVo = new TableVo();
        Table table = this.persistentClass.getAnnotation(Table.class);
        String name = table.name();
        if(CommonUtil.isEmpty(name)){
            name = this.persistentClass.getSimpleName().toLowerCase();
        }

        tableVo.setTable(name);

        List<ConditionVo> conditionVos = conditions(t);

        tableVo.setConditions(conditionVos);

        List<SelectPropertyVo> selectPropertys = selectPropertys(t.getClass());
        tableVo.setSelectPropertys(selectPropertys);

        return tableVo;
    }

    private List<SelectPropertyVo> selectPropertys(Class type)throws Exception{
        Class superClass = type.getSuperclass();
        if(superClass==null||superClass.equals(Object.class)){
            superClass = type;
        }

        Field[] fields = superClass.getDeclaredFields();

        Points points = (Points) type.getAnnotation(Points.class);
        List<String> propertyList = new ArrayList<>();

        if(CommonUtil.isNotEmpty(points)){
            String[] properties = points.properties();
            for(String property:properties){
                propertyList.add(property);
            }
        }

        List<SelectPropertyVo> selectPropertys = new ArrayList<>();
        int i = 0;

        for(Field field:fields){
            Column column = field.getAnnotation(Column.class);
            if(CommonUtil.isNotEmpty(column)){
                int mode = column.mode();
                if(mode==0||propertyList.contains(field.getName())){
                    i++;
                    String name = column.name();
                    String fun = column.fun();
                    if(CommonUtil.isEmpty(name)){
                        name = field.getName().toLowerCase();
                    }else{

                    }

                    SelectPropertyVo selectProperty = new SelectPropertyVo();
                    selectProperty.setColumn(name);
                    selectProperty.setFun(fun);
                    selectProperty.setName(field.getName());
                    selectProperty.setAsName(name+"_"+i);
                    selectPropertys.add(selectProperty);
                }
            }
        }

        return selectPropertys;
    }

    private List<ConditionVo> conditions(T t)throws Exception{
        List<ConditionVo> conditionVos = new ArrayList<>();
        Field[] fields = this.persistentClass.getDeclaredFields();
        for(Field field:fields){
            List<Condition> conditions = new ArrayList<>();
            Conditions conditionsAnn = field.getAnnotation(Conditions.class);
            Condition conditionAnn = field.getAnnotation(Condition.class);
            if(CommonUtil.isNotEmpty(conditionsAnn)){
                for(Condition condition:conditionsAnn.value()){
                    conditions.add(condition);
                }
            }

            if(CommonUtil.isNotEmpty(conditionAnn)){
                conditions.add(conditionAnn);
            }

            Column column = field.getAnnotation(Column.class);

            if(CommonUtil.isNotEmpty(column)){
                String name = column.name();
                if(CommonUtil.isEmpty(name)){
                    name = field.getName().toLowerCase();
                }

                for(Condition condition:conditions){
                    String[] properties = condition.properties();
                    String fun = condition.fun();
                    String[] funParams = condition.funParams();

                    if(CommonUtil.isNotEmpty(properties)&&properties.length>0){
                        for(String property:properties){
                            try{
                                if(property.startsWith("&")){
                                    Object value = field.get(t);
                                    field.setAccessible(true);
                                    ConditionVo conditionVo = new ConditionVo();
                                    conditionVo.setColumn(property.substring(1));
                                    conditionVo.setType(condition.type());
                                    conditionVo.setValue(value);

                                    if(CommonUtil.isNotEmpty(fun)){
                                        conditionVo.setFun(fun);
                                    }

                                    if(CommonUtil.isNotEmpty(funParams)&&funParams.length>0){
                                        List<String> funParamList = new ArrayList<>();
                                        for(String funParam:funParams){
                                            funParamList.add(funParam);
                                        }

                                        conditionVo.setFunParams(funParamList);
                                    }

                                    conditionVos.add(conditionVo);

                                }else{
                                    Field propField = t.getClass().getDeclaredField(property);
                                    propField.setAccessible(true);
                                    Object value = propField.get(t);
                                    if(CommonUtil.isNotEmpty(value)){
                                        ConditionVo conditionVo = new ConditionVo();
                                        conditionVo.setColumn(name);
                                        conditionVo.setType(condition.type());
                                        conditionVo.setValue(value);
                                        if(CommonUtil.isNotEmpty(fun)){
                                            conditionVo.setFun(fun);
                                        }

                                        if(CommonUtil.isNotEmpty(funParams)&&funParams.length>0){
                                            List<String> funParamList = new ArrayList<>();
                                            for(String funParam:funParams){
                                                funParamList.add(funParam);
                                            }
                                            conditionVo.setFunParams(funParamList);
                                        }
                                        conditionVos.add(conditionVo);
                                    }
                                }

                            }catch (Exception e){
                                throw new RuntimeException(e);
                            }
                        }
                    }else{
                        field.setAccessible(true);
                        Object value = field.get(t);
                        if(CommonUtil.isNotEmpty(value)){
                            ConditionVo conditionVo = new ConditionVo();
                            conditionVo.setColumn(name);
                            conditionVo.setType(condition.type());
                            conditionVo.setValue(value);
                            conditionVos.add(conditionVo);
                        }
                    }
                }
            }
        }
        return conditionVos;
    }

    private static Class<Object> getSuperClassGenricType(final Class clazz,final int index){
        Type genType = clazz.getGenericSuperclass();
        if(!(genType instanceof ParameterizedType)){
            return Object.class;
        }

        Type[] params = ((ParameterizedType)genType).getActualTypeArguments();
        if(index>=params.length||index<0){
            return Object.class;
        }

        if(!(params[index] instanceof Class)){
            return Object.class;
        }
        return (Class) params[index];
    }

}
