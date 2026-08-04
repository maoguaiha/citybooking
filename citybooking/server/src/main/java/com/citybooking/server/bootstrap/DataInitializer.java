package com.citybooking.server.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryMapper categoryMapper;

    public DataInitializer(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public void run(String... args) {
        if (categoryMapper.selectCount(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, 0L)) > 0) {
            return;
        }
        List<Category> roots = List.of(
                category("家政保洁"), category("家电维修"), category("搬家服务"),
                category("上门按摩"), category("宠物服务"), category("美容美甲"));
        roots.forEach(categoryMapper::insert);
    }

    private Category category(String name) {
        Category c = new Category();
        c.setName(name);
        c.setParentId(0L);
        c.setSort(0);
        return c;
    }
}
