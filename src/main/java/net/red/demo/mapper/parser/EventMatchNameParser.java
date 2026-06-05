package net.red.demo.mapper.parser;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.common.exception.StreamException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventMatchNameParser {
    private static final Pattern MATCH_NAME_PATTERN = Pattern.compile("(.+) [vV][sS]? (.+)");

    /**
     * Parse matchName value to get home and away competitors
     *
     * @param matchName string to parse
     * @return pair, where first element is home competitor, second element is away competitor
     * @throws StreamException when matchName is invalid to parse home and away competitors
     */
    @Nonnull
    public Pair<String, String> parse(@Nullable String matchName) {
        var matcherOptional = getMatcher(matchName);
        if (matcherOptional.isEmpty()) {
            var errMsg = "Event matchName is blank";
            log.error(errMsg);
            throw new StreamException(errMsg);
        }
        var matcher = matcherOptional.get();
        if (!matcher.matches()) {
            var errMsg = MessageFormat.format("Unexpected Event matchName to parse: ''{0}''", matchName);
            log.error(errMsg);
            throw new StreamException(errMsg);
        }
        return Pair.of(StringUtils.normalizeSpace(matcher.group(1)), StringUtils.normalizeSpace(matcher.group(2).trim()));
    }

    public boolean isMatchNameParseable(@Nullable String matchName) {
        var matcher = getMatcher(matchName);
        return matcher.map(Matcher::find).orElse(false);
    }

    private Optional<Matcher> getMatcher(@Nullable String matchName) {
        if (StringUtils.isBlank(matchName)) {
            return Optional.empty();
        }
        return Optional.of(MATCH_NAME_PATTERN.matcher(matchName));
    }
}