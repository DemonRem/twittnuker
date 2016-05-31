/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import de.vanita5.twittnuker.model.SpanItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.text.TextUtils.isEmpty;
import static de.vanita5.twittnuker.util.HtmlEscapeHelper.escape;
import static de.vanita5.twittnuker.util.HtmlEscapeHelper.unescape;

public class HtmlBuilder {

    private final CodePointArray source;
    private final int sourceLength;
    private final boolean throwExceptions, sourceIsEscaped, shouldReEscape;

    private final ArrayList<LinkSpec> links = new ArrayList<>();

    public HtmlBuilder(final String source, final boolean strict, final boolean sourceIsEscaped,
                       final boolean shouldReEscape) {
        this(new CodePointArray(source), strict, sourceIsEscaped, shouldReEscape);
    }

    public HtmlBuilder(final CodePointArray source, final boolean strict, final boolean sourceIsEscaped,
                       final boolean shouldReEscape) {
        if (source == null) throw new NullPointerException();
        this.source = source;
        this.sourceLength = source.length();
        this.throwExceptions = strict;
        this.sourceIsEscaped = sourceIsEscaped;
        this.shouldReEscape = shouldReEscape;
    }

    public boolean addLink(final String link, final String display, final int start, final int end) {
        return addLink(link, display, start, end, false);
    }

    public boolean addLink(final String link, final String display, final int start, final int end,
                           final boolean display_is_html) {
        if (start < 0 || end < 0 || start > end || end > sourceLength) {
            final String message = String.format(Locale.US, "text:%s, length:%d, start:%d, end:%d", source,
                    sourceLength, start, end);
            if (throwExceptions) throw new StringIndexOutOfBoundsException(message);
            return false;
        }
        if (hasLink(start, end)) {
            final String message = String.format(Locale.US,
                    "link already added in this range! text:%s, link:%s, display:%s, start:%d, end:%d", source, link,
                    display, start, end);
            if (throwExceptions) throw new IllegalArgumentException(message);
            return false;
        }
        return links.add(new LinkSpec(link, display, start, end, display_is_html));
    }

    public String build() {
        if (links.isEmpty()) return escapeSource();
        Collections.sort(links);
        final StringBuilder sb = new StringBuilder();
        final int linksSize = links.size();
        for (int i = 0; i < linksSize; i++) {
            final LinkSpec spec = links.get(i);
            if (spec == null) {
                continue;
            }
            final int start = spec.start, end = spec.end;
            if (i == 0) {
                if (start >= 0 && start <= sourceLength) {
                    appendSource(sb, 0, start, shouldReEscape, sourceIsEscaped);
                }
            } else if (i > 0) {
                final int lastEnd = links.get(i - 1).end;
                if (lastEnd >= 0 && lastEnd <= start && start <= sourceLength) {
                    appendSource(sb, lastEnd, start, shouldReEscape, sourceIsEscaped);
                }
            }
            sb.append("<a href=\"");
            sb.append(spec.link);
            sb.append("\">");
            if (start >= 0 && start <= end && end <= sourceLength) {
                if (isEmpty(spec.display)) {
                    append(sb, spec.link, shouldReEscape, false);
                } else {
                    append(sb, spec.display, shouldReEscape, spec.displayIsHtml);
                }
            }
            sb.append("</a>");
            if (i == linksSize - 1 && end >= 0 && end <= sourceLength) {
                appendSource(sb, end, sourceLength, shouldReEscape, sourceIsEscaped);
            }
        }
        return sb.toString();
    }

    public Pair<String, List<SpanItem>> buildWithIndices() {
        List<SpanItem> items = new ArrayList<>();
        if (links.isEmpty()) return Pair.create(escapeSource(), items);
        Collections.sort(links);
        final StringBuilder sb = new StringBuilder();
        final int linksSize = links.size();
        for (int i = 0; i < linksSize; i++) {
            final LinkSpec spec = links.get(i);
            if (spec == null) {
                continue;
            }
            final int start = spec.start, end = spec.end;
            if (i == 0) {
                if (start >= 0 && start <= sourceLength) {
                    appendSource(sb, 0, start, false, sourceIsEscaped);
                }
            } else if (i > 0) {
                final int lastEnd = links.get(i - 1).end;
                if (lastEnd >= 0 && lastEnd <= start && start <= sourceLength) {
                    appendSource(sb, lastEnd, start, false, sourceIsEscaped);
                }
            }
            int spanStart = sb.length();
            if (start >= 0 && start <= end && end <= sourceLength) {
                if (isEmpty(spec.display)) {
                    append(sb, spec.link, false, false);
                } else {
                    append(sb, spec.display, false, spec.displayIsHtml);
                }
            }
            final SpanItem item = new SpanItem();
            item.start = spanStart;
            item.end = sb.length();
            item.link = spec.link;
            items.add(item);
            if (i == linksSize - 1 && end >= 0 && end <= sourceLength) {
                appendSource(sb, end, sourceLength, false, sourceIsEscaped);
            }
        }
        return Pair.create(sb.toString(), items);
    }

    public boolean hasLink(final int start, final int end) {
        for (final LinkSpec spec : links) {
            if (start >= spec.start && start <= spec.end || end >= spec.start && end <= spec.end)
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "HtmlBuilder{" +
                ", codePoints=" + source +
                ", codePointsLength=" + sourceLength +
                ", throwExceptions=" + throwExceptions +
                ", sourceIsEscaped=" + sourceIsEscaped +
                ", shouldReEscape=" + shouldReEscape +
                ", links=" + links +
                '}';
    }

    private void appendSource(final StringBuilder builder, final int start, final int end, boolean escapeSource, boolean sourceEscaped) {
        if (sourceEscaped == escapeSource) {
            append(builder, source.substring(start, end), escapeSource, sourceEscaped);
        } else if (escapeSource) {
            append(builder, escape(source.substring(start, end)), true, sourceEscaped);
        } else {
            append(builder, unescape(source.substring(start, end)), false, sourceEscaped);
        }
    }

    private void append(final StringBuilder builder, final String text, boolean escapeText, boolean textEscaped) {
        if (textEscaped == escapeText) {
            builder.append(text);
        } else if (escapeText) {
            builder.append(escape(text));
        } else {
            builder.append(unescape(text));
        }
    }

    private String escapeSource() {
        final String source = this.source.substring(0, this.source.length());
        if (sourceIsEscaped == shouldReEscape) return source;
        return shouldReEscape ? escape(source) : unescape(source);
    }

    static final class LinkSpec implements Comparable<LinkSpec> {

        final String link, display;
        final int start, end;
        final boolean displayIsHtml;

        LinkSpec(final String link, final String display, final int start, final int end, final boolean displayIsHtml) {
            this.link = link;
            this.display = display;
            this.start = start;
            this.end = end;
            this.displayIsHtml = displayIsHtml;
        }

        @Override
        public int compareTo(@NonNull final LinkSpec that) {
            return start - that.start;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof LinkSpec)) return false;
            final LinkSpec other = (LinkSpec) obj;
            if (display == null) {
                if (other.display != null) return false;
            } else if (!display.equals(other.display)) return false;
            if (displayIsHtml != other.displayIsHtml) return false;
            if (end != other.end) return false;
            if (link == null) {
                if (other.link != null) return false;
            } else if (!link.equals(other.link)) return false;
            if (start != other.start) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (display == null ? 0 : display.hashCode());
            result = prime * result + (displayIsHtml ? 1231 : 1237);
            result = prime * result + end;
            result = prime * result + (link == null ? 0 : link.hashCode());
            result = prime * result + start;
            return result;
        }

        @Override
        public String toString() {
            return "LinkSpec{" +
                    "link='" + link + '\'' +
                    ", display='" + display + '\'' +
                    ", start=" + start +
                    ", end=" + end +
                    ", displayIsHtml=" + displayIsHtml +
                    '}';
        }
    }

}