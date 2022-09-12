package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NamedLockStockFacadeTest {
    @Autowired
    private NamedLockStockFacade namedLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before(){
        Stock stock = new Stock(1L, 100l);

        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after(){
        stockRepository.deleteAll();
    }


    @Test
    public void 동시에_100개의_요청() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for(int i=0; i<threadCount; i++){
            executorService.submit(()->{
                try {
                    // 아무 처리 안 된 상태에서 여러 쓰레드에서 동시에 재고 값 감소시키는 로직 수행
                    namedLockStockFacade.decrease(1l, 1l);
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 100 - (1*100) = 0 기대
        assertEquals(0,stock.getQuantity());
    }
}