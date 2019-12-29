package com.zjmxdz.service;

import com.wyc.common.service.BaseAbstractService;
import com.zjmxdz.domain.entity.VPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class VPageService extends BaseAbstractService<VPage> {
    @Autowired
    private DataSource dataSource;
    @Override
    protected DataSource dataSource() {
        return dataSource;
    }

    @Override
    protected String database() {
        return "mysql";
    }
}
