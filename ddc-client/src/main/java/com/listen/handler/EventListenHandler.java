package com.listen.handler;

import com.listen.dto.EventListenHandlerDTO;

/**
 * @author baofeng
 * @date 2023/07/04
 */
public interface EventListenHandler {
    void handle(EventListenHandlerDTO dto);
}
