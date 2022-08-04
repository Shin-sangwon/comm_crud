package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArticleServiceTest {
    private MyMap myMap;
    private ArticleService articleService;
    private static final int TEST_DATA_SIZE = 30;
    ArticleServiceTest(){
       myMap = Container.getObj(MyMap.class);
       articleService = Container.getObj(ArticleService.class);
    }

    @BeforeAll // 테스트 실행 전 한번 실행
    public void BeforeAll() {

        // 모든 DB 처리시에, 처리되는 SQL을 콘솔에 출력
        myMap.setDevMode(true);
    }

    @BeforeEach //메소드 실행 전 무조건 먼저 실행
    public void beforeEach() {
        // 게시물 테이블을 깔끔하게 삭제한다.
        // DELETE FROM article; // 보다 TRUNCATE article; 로 삭제하는게 더 깔끔하고 흔적이 남지 않는다.
        truncateArticleTable();
        // 게시물 3개를 만든다.
        // 테스트에 필요한 샘플데이터를 만든다고 보면 된다.
        makeArticleTestData();
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, TEST_DATA_SIZE).forEach(no -> {
            boolean isBlind = no >= 11 && no <= 20;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            myMap.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        // 테이블을 깔끔하게 지워준다.
        myMap.run("TRUNCATE article");
    }

    @Test
    public void 존재한다() {
        assertThat(articleService).isNotNull();
    }

    @Test
    public void getArticles() {

        List<ArticleDto> articleDtoList = articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void getArticleById(){
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1L);
        assertThat(articleDto.getTitle()).isEqualTo("제목1");
        assertThat(articleDto.getBody()).isEqualTo("내용1");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();

    }

    @Test
    public void getArticlesCount(){
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(TEST_DATA_SIZE);

    }

    @Test
    public void write(){
        long newArticleId = articleService.write("제목 new", "내용 new", false);

        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isEqualTo(false);


    }

    @Test
    public void modify() {
        //Ut.sleep(5000);

        articleService.modify(1, "제목 new", "내용 new", true);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.isBlind()).isEqualTo(true);

        // DB에서 받아온 게시물 수정날짜와 자바에서 계산한 현재 날짜를 비교하여(초단위)
        // 그것이 1초 이하로 차이가 난다면
        // 수정날짜가 갱신되었다 라고 볼 수 있음
        long diffSeconds = ChronoUnit.SECONDS.between(articleDto.getModifiedDate(), LocalDateTime.now());
        assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }

    @Test
    public void delete(){

        articleService.delete(1);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto).isNull();

    }

    @Test
    public void 객체로_전달하여_다음글을_가져온다(){

        ArticleDto articleDto = articleService.getArticleById(30);
        ArticleDto articleDto2 = articleService.getNextArticle(articleDto);
        assertThat(articleDto2).isNull();

    }

    @Test
    public void 객체로_전달하여_이전글을_가져온다(){

        ArticleDto articleDto = articleService.getArticleById(2);
        ArticleDto articleDto2 = articleService.getPreviousArticle(articleDto);

        assertThat(articleDto2.getId()).isEqualTo(1L);
        assertThat(articleDto2.getTitle()).isEqualTo("제목1");
        assertThat(articleDto2.getBody()).isEqualTo("내용1");
        assertThat(articleDto2.getCreatedDate()).isNotNull();
        assertThat(articleDto2.getModifiedDate()).isNotNull();
        assertThat(articleDto2.isBlind()).isFalse();
    }

    @Test
    public void ID를_전달하여_다음글을_가져온다(){

        ArticleDto articleDto2 = articleService.getNextArticle(3);
        assertThat(articleDto2.getId()).isEqualTo(4L);
        assertThat(articleDto2.getTitle()).isEqualTo("제목4");
        assertThat(articleDto2.getBody()).isEqualTo("내용4");
        assertThat(articleDto2.getCreatedDate()).isNotNull();
        assertThat(articleDto2.getModifiedDate()).isNotNull();
        assertThat(articleDto2.isBlind()).isFalse();

    }

    @Test
    public void ID를_전달하여_이전글을_가져온다(){

        ArticleDto articleDto2 = articleService.getPreviousArticle(1);
        assertThat(articleDto2).isNull();

    }

    @Test
    public void 블라인드된_글은_표시하지_않기(){
        //11번~20번 블라인드 처리
        ArticleDto articleDto2 = articleService.getNextArticle(10);
        assertThat(articleDto2.getId()).isEqualTo(21L);
        assertThat(articleDto2.getTitle()).isEqualTo("제목21");
        assertThat(articleDto2.getBody()).isEqualTo("내용21");
        assertThat(articleDto2.getCreatedDate()).isNotNull();
        assertThat(articleDto2.getModifiedDate()).isNotNull();
        assertThat(articleDto2.isBlind()).isFalse();


    }


}