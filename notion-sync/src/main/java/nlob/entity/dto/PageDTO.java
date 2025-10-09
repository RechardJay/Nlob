package nlob.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 页面块内容查询
 */
@NoArgsConstructor
@Data
public class PageDTO {

    private String object;
    private List<ResultsDTO> results;
    private Object nextCursor;
    private Boolean hasMore;
    private String type;
    private BlockDTO block;
    private String requestId;

    @NoArgsConstructor
    @Data
    public static class BlockDTO {
    }

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
        private Heading1DTO heading1;
        private BulletedListItemDTO bulletedListItem;
        private NumberedListItemDTO numberedListItem;
        private ToDoDTO toDo;
        private ToggleDTO toggle;
        private ChildPageDTO childPage;
        private CalloutDTO callout;
        private QuoteDTO quote;
        private TableDTO table;
        private CodeDTO code;
        private ImageDTO image;

        @NoArgsConstructor
        @Data
        public static class ParentDTO {
            private String type;
            private String pageId;
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

        @NoArgsConstructor
        @Data
        public static class Heading1DTO {
            private List<RichTextDTO> richText;
            private Boolean isToggleable;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class BulletedListItemDTO {
            private List<RichTextDTO> richText;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class NumberedListItemDTO {
            private List<RichTextDTO> richText;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class ToDoDTO {
            private List<RichTextDTO> richText;
            private Boolean checked;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class ToggleDTO {
            private List<RichTextDTO> richText;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class ChildPageDTO {
            private String title;
        }

        @NoArgsConstructor
        @Data
        public static class CalloutDTO {
            private List<RichTextDTO> richText;
            private IconDTO icon;
            private String color;

            @NoArgsConstructor
            @Data
            public static class IconDTO {
                private String type;
                private String emoji;
            }

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class QuoteDTO {
            private List<RichTextDTO> richText;
            private String color;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class TableDTO {
            private Integer tableWidth;
            private Boolean hasColumnHeader;
            private Boolean hasRowHeader;
        }

        @NoArgsConstructor
        @Data
        public static class CodeDTO {
            private List<?> caption;
            private List<RichTextDTO> richText;
            private String language;

            @NoArgsConstructor
            @Data
            public static class RichTextDTO {
                private String type;
                private TextDTO text;
                private ParagraphDTO.RichTextDTO.AnnotationsDTO annotations;
                private String plainText;
                private Object href;

                @NoArgsConstructor
                @Data
                public static class TextDTO {
                    private String content;
                    private Object link;
                }
            }
        }

        @NoArgsConstructor
        @Data
        public static class ImageDTO {
            private List<?> caption;
            private String type;
            private FileDTO file;

            @NoArgsConstructor
            @Data
            public static class FileDTO {
                private String url;
                private String expiryTime;
            }
        }
    }
}