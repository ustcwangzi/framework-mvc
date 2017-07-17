package com.wz.service.impl;

import com.wz.annotation.Service;
import com.wz.service.ModifyService;

/**
 * Created by wz on 2017-07-17.
 */
@Service("modify")
public class ModifyServiceImpl implements ModifyService {
    @Override
    public String add(String name, Integer age) {
        return "invoke add " + name + "," + age;
    }

    @Override
    public String update(String name) {
        return "invoke update " + name;
    }
}
