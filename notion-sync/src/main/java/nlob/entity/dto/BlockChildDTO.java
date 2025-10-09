package nlob.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class BlockChildDTO {
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
        private ParentDTO parent;
        private String createdTime;
        private String lastEditedTime;
        private CreatedByDTO createdBy;
        private CreatedByDTO lastEditedBy;
        private Boolean hasChildren;
        private Boolean archived;
        private Boolean inTrash;
        private String type;
        private ParagraphDTO paragraph;

        @NoArgsConstructor
        @Data
        public static class ParentDTO {
            private String type;
            private String blockId;
        }

        @NoArgsConstructor
        @Data
        public static class CreatedByDTO {
            private String object;
            private String id;
        }

        @NoArgsConstructor
        @Data
        public static class ParagraphDTO {
            private List<RichTextDTO> richText;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
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
