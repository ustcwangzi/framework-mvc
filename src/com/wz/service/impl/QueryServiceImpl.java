package com.wz.service.impl;

import com.wz.annotation.Service;
import com.wz.service.QueryService;

/**
 * Created by wz on 2017-07-17.
 */
@Service
public class QueryServiceImpl implements QueryService{
    @Override
    public String search(String name) {
        return "invoke search " + name;
    }
}
