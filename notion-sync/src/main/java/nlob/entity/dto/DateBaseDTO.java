package nlob.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据库查询的返回结果
 */
@NoArgsConstructor
@Data
public class DateBaseDTO {

    private String object;
    private List<ResultsDTO> results;
    private Object nextCursor;
    private Boolean hasMore;
    private String type;
    private String requestId;

    @NoArgsConstructor
    @Data
    public static class ResultsDTO {
        private String object;
        private String id;
        private String createdTime;
        private String lastEditedTime;
        private CreatedByDTO createdBy;
        private CreatedByDTO lastEditedBy;
        private Object cover;
        private Object icon;
        private ParentDTO parent;
        private Boolean archived;
        private Boolean inTrash;
        private Boolean isLocked;
        private PropertiesDTO properties;
        private String url;
        private String publicUrl;

        @NoArgsConstructor
        @Data
        public static class CreatedByDTO {
            private String object;
            private String id;
        }

        @NoArgsConstructor
        @Data
        public static class ParentDTO {
            private String type;
            private String databaseId;
        }

        @NoArgsConstructor
        @Data
        public static class PropertiesDTO {
            private 多选DTO 多选;
            private 名称DTO 名称;

            @NoArgsConstructor
            @Data
            public static class 多选DTO {
                private String id;
                private String type;
                private List<MultiSelectDTO> multiSelect;

                @NoArgsConstructor
                @Data
                public static class MultiSelectDTO {
                    private String id;
                    private String name;
                    private String color;
                }
            }

            @NoArgsConstructor
            @Data
            public static class 名称DTO {
                private String id;
                private String type;
                private List<TitleDTO> title;

                @NoArgsConstructor
                @Data
                public static class TitleDTO {
                    private String type;
                    private TextDTO text;
                    private AnnotationsDTO annotations;
                    private String plainText;
                    private Object href;

                    @NoArgsConstructor
                    @Data
                    public static class TextDTO {
                        private String content;
                        private Object link;
                    }

                    @NoArgsConstructor
                    @Data
                    public static class AnnotationsDTO {
                        private Boolean bold;
                        private Boolean italic;
                        private Boolean strikethrough;
                        private Boolean underline;
                        private Boolean code;
                        private String color;
                    }
                }
            }
        }
    }
}
