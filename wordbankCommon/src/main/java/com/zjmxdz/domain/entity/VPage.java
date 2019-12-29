package com.zjmxdz.domain.entity;

import com.wyc.common.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@Table(name = "v_page")
public class VPage {
    @Id(generator = Id.Generator.UUID)
    @Column(name="id")
    private String id;
    @Condition(properties = "codeCondition")
    @Column(name="code")
    private String code;
    @Column(name="page")
    private String page;
    @CreateAt
    @Column(name="create_at")
    private Date createAt;
    @UpdateAt
    @Column(name="update_at")
    private Date updateAt;
}
