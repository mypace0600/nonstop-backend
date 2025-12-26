package com.app.nonstop.mapper;

import com.app.nonstop.domain.file.entity.File;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper {
    void save(File file);
}
