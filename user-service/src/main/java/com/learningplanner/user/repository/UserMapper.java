package com.learningplanner.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
