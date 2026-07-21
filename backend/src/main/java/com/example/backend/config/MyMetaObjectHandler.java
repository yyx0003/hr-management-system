package com.example.backend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * created_at／updated_atを自動的に設定するハンドラ。
 * 各Entityの該当フィールドに@TableField(fill = FieldFill.INSERT)
 * または@TableField(fill = FieldFill.INSERT_UPDATE)を付与しておけば、
 * insert()／update()実行時にこのクラスが自動的に日時をセットする。
 * JPAの@CreationTimestamp／@UpdateTimestampに相当する仕組み。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}