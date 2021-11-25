package com.shop.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.constant.ItemSellStatus;
import com.shop.dto.*;
import com.shop.entity.Item;
import com.shop.entity.QCategory;
import com.shop.entity.QItem;
import com.shop.entity.QItemImg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class ItemRepositoryCustomImpl implements ItemRepositoryCustom{    // ItemRepositoryCustom 상속

    private JPAQueryFactory queryFactory;                    // 동적 쿼리 생성을 위해 사용

    public ItemRepositoryCustomImpl(EntityManager em){       // JPAQueryFactory 생성자로 Em 객체 넣어줌
        this.queryFactory = new JPAQueryFactory(em);
    }

    // 상품 판매 상태 조건 유무에 따라 조회
    private BooleanExpression searchSellStatusEq(ItemSellStatus searchSellStatus){
        return searchSellStatus ==
                null ? null : QItem.item.itemSellStatus.eq(searchSellStatus);
    }

    // dateTime의 값의 이전 시간 값으로 세팅 후 해당 시간 이후로 조회
    private BooleanExpression regDtsAfter(String searchDateType){
        LocalDateTime dateTime = LocalDateTime.now();

        if(StringUtils.equals("all", searchDateType) || searchDateType == null){
            return null;
        } else if(StringUtils.equals("1d", searchDateType)){
            dateTime = dateTime.minusDays(1);
        }else if(StringUtils.equals("1w", searchDateType)){
            dateTime = dateTime.minusWeeks(1);
        }else if(StringUtils.equals("1m", searchDateType)){
            dateTime = dateTime.minusMonths(1);
        }else if(StringUtils.equals("6m", searchDateType)){
            dateTime = dateTime.minusMonths(1);
        }
        return QItem.item.regTime.after(dateTime);
    }

    // 상품명 또는 상품 생성자 아이디 또는 카테고리 에 검색어 포함 시 조회
    private BooleanExpression searchByLike(String searchBy, String searchQuery){

        if(StringUtils.equals("itemNm", searchBy)){
            return QItem.item.itemNm.like("%" + searchQuery + "%");
        }else if(StringUtils.equals("createBy", searchBy)){
            return QItem.item.createdBy.like("%" + searchQuery + "%");
        }else if(StringUtils.equals("cateName", searchBy)){
            return QCategory.category.cateName.like("%"+ searchQuery + "%");
        }
        return null;
    }


    @Override
    public Page<Item> getAdminItemPage(ItemSearchDto itemSearchDto, Pageable pageable) {

        // queryFactory를 이용해 쿼리 생성
        QueryResults<Item> results = queryFactory
                .selectFrom(QItem.item)            // 상품데이터 조회를 위해 Qitem의 item 지정
                .where(regDtsAfter(itemSearchDto.getSearchDateType()),
                        searchSellStatusEq(itemSearchDto.getSearchSellStatus()),
                        searchByLike(itemSearchDto.getSearchBy(),
                        itemSearchDto.getSearchQuery()))
                .orderBy(QItem.item.id.desc())
                .offset(pageable.getOffset())     // 시작 인덱스 지정
                .limit(pageable.getPageSize())    // 한번에 가지고 올 최대 개수 지정
                .fetchResults();                  // 조회한 리스트, 전체 개수를 포함하는 QueryResults 반환
                                                  // 2개의 쿼리문이 실행됨

        List<Item> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    // 검색어가 null이 아닐 때 해당 검색어가 포함되는 상품 조회
    private BooleanExpression itemNmLike(String searchQuery){
        return StringUtils.isEmpty(searchQuery) ?
                null : QItem.item.itemNm.like("%" + searchQuery + "%");
    }


    @Override
    public Page<MainItemDto> getMainItemPage(ItemSearchDto itemSearchDto,
                                             Pageable pageable) {
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;

        QueryResults<MainItemDto> results = queryFactory
                .select(
                        new QMainItemDto(
                                item.id,
                                item.itemNm,
                                item.itemDetail,
                                itemImg.imgUrl,
                                item.price)
                        )
                .from(itemImg)
                .join(itemImg.item, item)
                .where(itemImg.repimgYn.eq("Y"))
                .where(itemNmLike(itemSearchDto.getSearchQuery()))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MainItemDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);

    }

    @Override
    public Page<GiftMainItemDto> getGiftItemPage(ItemSearchDto itemSearchDto,
                                             Pageable pageable, Long cateCode) {
        QItem item = QItem.item;
        QItemImg itemImg = QItemImg.itemImg;
        QCategory category = QCategory.category;

        QueryResults<GiftMainItemDto> results = queryFactory
                .select(
                        new QGiftMainItemDto(
                                item.id,
                                category.cateCode,
                                item.itemNm,
                                item.itemDetail,
                                itemImg.imgUrl,
                                item.price)
                )
                .from(itemImg)
                .join(itemImg.item, item)
                .join(item.category, category)
                .where(itemImg.repimgYn.eq("Y"))
                .where(itemNmLike(itemSearchDto.getSearchQuery()))
                .where(category.cateCode.eq(cateCode))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<GiftMainItemDto> content = results.getResults();

        log.info(content.toString());

        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);

    }

}
