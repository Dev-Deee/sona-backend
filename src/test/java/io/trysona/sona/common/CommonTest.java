package io.trysona.sona.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.trysona.sona.exception.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CommonTest {

    @Test
    void pageRequestsValidateBounds() {
        assertThat(PageRequests.of(0, 1, Sort.unsorted()).getPageSize()).isEqualTo(1);
        assertThat(PageRequests.of(3, 100, Sort.unsorted()).getPageNumber()).isEqualTo(3);
        assertThatThrownBy(() -> PageRequests.of(-1, 20, Sort.unsorted()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("page");
        assertThatThrownBy(() -> PageRequests.of(0, 0, Sort.unsorted()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("size");
        assertThatThrownBy(() -> PageRequests.of(0, 101, Sort.unsorted())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void jsonbHelpersHandleNulls() {
        assertThat(Jsonb.objectOrEmpty(null)).isEmpty();
        assertThat(Jsonb.arrayOrEmpty(null)).isEmpty();
        assertThat(Jsonb.arrayOrEmpty(List.of(Map.of("a", 1)))).hasSize(1);
        Map<String, Object> patch = new HashMap<>();
        patch.put("drop", null);
        patch.put("add", 2);
        assertThat(Jsonb.merge(Map.of("drop", 1, "keep", 3), patch)).containsOnlyKeys("keep", "add");
        assertThat(Jsonb.merge(null, Map.of("a", 1))).containsEntry("a", 1);
    }

    @Test
    void pageResponseMapsContent() {
        var page = new PageImpl<>(List.of(1, 2), PageRequest.of(1, 2), 6);
        PageResponse<String> response = PageResponse.of(page, String::valueOf);
        assertThat(response.content()).containsExactly("1", "2");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void apiResponseEnvelope() {
        assertThat(ApiResponse.ok("x").success()).isTrue();
        assertThat(ApiResponse.ok("x").message()).isEqualTo("OK");
        assertThat(ApiResponse.ok(null, "Done").message()).isEqualTo("Done");
    }
}
