package com.app.nonstop.mapper;

import com.app.nonstop.domain.user.entity.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginHistoryMapper {
    void save(LoginHistory loginHistory);
}
