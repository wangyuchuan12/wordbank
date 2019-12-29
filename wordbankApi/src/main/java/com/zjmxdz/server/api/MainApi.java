package com.zjmxdz.server.api;


import com.zjmxdz.domain.dto.VPageDto;
import com.zjmxdz.domain.entity.VPage;
import com.zjmxdz.service.VPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api")
public class MainApi {

    MainApi(){
        System.out.println("sss");
    }
    @Autowired
    private VPageService pageService;

    @RequestMapping("/test")
    public Object test(){
        VPage vPage = new VPage();
        vPage.setCode("code");
        vPage.setPage("222");
        pageService.add(vPage);

        Map<String,Object> map = new HashMap<>();
        map.put("name","wyc");
        return map;
    }

    @RequestMapping("/query")
    public Object query(HttpServletRequest httpServletRequest){

        VPageDto pageDto = new VPageDto();
        pageDto.setCodeCondition(httpServletRequest.getParameter("code"));

        VPage vPage = pageService.findOne(pageDto);

        return vPage;
    }

}
