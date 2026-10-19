package com.studyplanner.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页结果包装
 */
public class PageDto<T> {

    private List<T> list;        // 当前页数据
    private long total;          // 总条数
    private int page;            // 当前页码（从0开始）
    private int size;            // 每页条数
    private int totalPages;      // 总页数

    public PageDto() {}

    /**
     * 从 Spring Data 的 Page 构造，配合实体→DTO 转换函数
     */
    public static <E, T> PageDto<T> from(Page<E> page, Function<E, T> converter) {
        PageDto<T> dto = new PageDto<>();
        dto.setList(page.getContent().stream().map(converter).collect(Collectors.toList()));
        dto.setTotal(page.getTotalElements());
        dto.setPage(page.getNumber());
        dto.setSize(page.getSize());
        dto.setTotalPages(page.getTotalPages());
        return dto;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
