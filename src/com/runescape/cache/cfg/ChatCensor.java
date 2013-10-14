package com.runescape.cache.cfg;

import com.runescape.cache.Archive;
import com.runescape.net.Buffer;

/* ChatCensor - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */

public class ChatCensor {
    private static int[] fragments;
    private static char[][] badWords;
    private static byte[][][] badBytes;
    private static char[][] domains;
    private static char[][] topLevelDomains;
    private static int[] topLevelDomainsType;
    private static final String[] WHITELISTED_WORDS = {"cook", "cook's", "cooks", "seeks", "sheet", "woop", "woops",
            "faq", "noob", "noobs"};

    public static final void load(final Archive archive) {
        final Buffer fragmentsEnc = new Buffer(archive.getFile("fragmentsenc.txt"));
        final Buffer badEnc = new Buffer(archive.getFile("badenc.txt"));
        final Buffer domainEnc = new Buffer(archive.getFile("domainenc.txt"));
        final Buffer topLevelDomainsBuffer = new Buffer(archive.getFile("tldlist.txt"));
        ChatCensor.loadDictionaries(fragmentsEnc, badEnc, domainEnc, topLevelDomainsBuffer);
    }

    private static final void loadDictionaries(final Buffer fragmentsEnc, final Buffer badEnc, final Buffer domainEnc,
                                               final Buffer topLevelDomainsBuffer) {
        ChatCensor.loadBadEnc(badEnc);
        ChatCensor.loadDomainEnc(domainEnc);
        ChatCensor.loadFragmentsEnc(fragmentsEnc);
        ChatCensor.loadTopLevelDomains(topLevelDomainsBuffer);
    }

    private static final void loadTopLevelDomains(final Buffer buffer) {
        final int length = buffer.getInt();
        ChatCensor.topLevelDomains = new char[length][];
        ChatCensor.topLevelDomainsType = new int[length];

        for (int index = 0; index < length; index++) {
            ChatCensor.topLevelDomainsType[index] = buffer.getUnsignedByte();
            final char[] topLevelDomain = new char[buffer.getUnsignedByte()];

            for (int character = 0; character < topLevelDomain.length; character++) {
                topLevelDomain[character] = (char) buffer.getUnsignedByte();
            }

            ChatCensor.topLevelDomains[index] = topLevelDomain;
        }
    }

    private static final void loadBadEnc(final Buffer buffer) {
        final int length = buffer.getInt();
        ChatCensor.badWords = new char[length][];
        ChatCensor.badBytes = new byte[length][][];
        ChatCensor.loadBadWords(buffer, ChatCensor.badWords, ChatCensor.badBytes);
    }

    private static final void loadDomainEnc(final Buffer buffer) {
        final int length = buffer.getInt();
        ChatCensor.domains = new char[length][];
        ChatCensor.loadDomains(ChatCensor.domains, buffer);
    }

    private static final void loadFragmentsEnc(final Buffer buffer) {
        final int length = buffer.getInt();
        ChatCensor.fragments = new int[length];

        for (int index = 0; index < ChatCensor.fragments.length; index++) {
            ChatCensor.fragments[index] = buffer.getUnsignedLEShort();
        }
    }

    private static final void loadBadWords(final Buffer buffer, final char[][] badWords, final byte[][][] badBytes) {
        for (int index = 0; index < badWords.length; index++) {
            final char[] badWord = new char[buffer.getUnsignedByte()];
            for (int i = 0; i < badWord.length; i++) {
                badWord[i] = (char) buffer.getUnsignedByte();
            }

            badWords[index] = badWord;
            final byte[][] badByte = new byte[buffer.getUnsignedByte()][2];

            for (int i = 0; i < badByte.length; i++) {
                badByte[i][0] = (byte) buffer.getUnsignedByte();
                badByte[i][1] = (byte) buffer.getUnsignedByte();
            }

            if (badByte.length > 0) {
                badBytes[index] = badByte;
            }
        }
    }

    private static final void loadDomains(final char[][] cs, final Buffer buffer) {
        for (int index = 0; index < cs.length; index++) {
            char[] domainEnc = new char[buffer.getUnsignedByte()];
            for (int character = 0; character < domainEnc.length; character++) {
                domainEnc[character] = (char) buffer.getUnsignedByte();
            }
            cs[index] = domainEnc;
        }
    }

    private static final void formatLegalCharacters(final char[] chars) {
        int character = 0;

        for (int i = 0; i < chars.length; i++) {
            if (ChatCensor.isLegalCharacter(chars[i])) {
                chars[character] = chars[i];
            } else {
                chars[character] = ' ';
            }

            if (character == 0 || chars[character] != ' ' || chars[character - 1] != ' ') {
                character++;
            }
        }

        for (int i = character; i < chars.length; i++) {
            chars[i] = ' ';
        }
    }

    private static final boolean isLegalCharacter(final char c) {
        return ((c >= ' ' && c <= '\u007f') || c == '\n'
                || c == '\t' || c == '\u00a3' || c == '\u20ac');
    }

    public static final String censorString(final String string) {
        char[] censoredString = string.toCharArray();
        ChatCensor.formatLegalCharacters(censoredString);

        final String censoredStringTrimmed = new String(censoredString).trim();
        final String censoredStringLowercase = censoredStringTrimmed.toLowerCase();
        censoredString = censoredStringTrimmed.toLowerCase().toCharArray();

        ChatCensor.method193(censoredString);
        ChatCensor.method188(censoredString, true);
        ChatCensor.handleEmails(censoredString);
        ChatCensor.method202(censoredString);

        for (String allowedWord : ChatCensor.WHITELISTED_WORDS) {
            int idx = -1;
            while ((idx = censoredStringLowercase.indexOf(allowedWord, idx + 1)) != -1) {
                final char[] chars = allowedWord.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    censoredString[i + idx] = chars[i];
                }
            }
        }

        ChatCensor.mergeCensoredStrings(censoredString, censoredStringTrimmed.toCharArray());
        ChatCensor.method187(censoredString);

        return new String(censoredString).trim();
    }

    private static final void mergeCensoredStrings(final char[] chars, final char[] trimmed) {
        for (int i = 0; i < trimmed.length; i++) {
            if (chars[i] != '*' && ChatCensor.isUpperCase(trimmed[i])) {
                chars[i] = trimmed[i];
            }
        }
    }

    private static final void method187(final char[] chars) {
        boolean bool = true;
        for (int j = 0; j < chars.length; j++) {
            final char c = chars[j];

            if (ChatCensor.isLetter(c)) {
                if (bool) {
                    bool = !ChatCensor.isLowerCase(c);
                }

                if (ChatCensor.isUpperCase(c)) {
                    chars[j] = (char) (c + 'a' - 'A');
                }
            } else {
                bool = true;
            }
        }
    }

    private static final void method188(final char[] chars, final boolean censor) {
        if (censor) {
            for (int i = 0; i < 2; i++) {
                for (int j = ChatCensor.badWords.length - 1; j >= 0; j--) {
                    ChatCensor.method197(ChatCensor.badBytes[j], chars, ChatCensor.badWords[j]);
                }
            }
        }
    }

    private static final void handleEmails(final char[] chars) {
        final char[] charsAtCopy = chars.clone();
        final char[] charsDotCopy = chars.clone();
        final char[] AT_SIGN_CHARS = {'(', 'a', ')'};
        final char[] DOT_CHARS = {'d', 'o', 't'};

        ChatCensor.method197(null, charsAtCopy, AT_SIGN_CHARS);
        ChatCensor.method197(null, charsDotCopy, DOT_CHARS);

        for (int i = ChatCensor.domains.length - 1; i >= 0; i--) {
            ChatCensor.method190(chars, ChatCensor.domains[i], charsDotCopy, charsAtCopy);
        }
    }

    private static final void method190(final char[] chars, final char[] domain, final char[] cs_34_, final char[] cs_35_) {
        if (domain.length <= chars.length) {
            int i_37_;
            for (int j = 0; j <= chars.length - domain.length; j += i_37_) {
                int idx = j;
                int i_39_ = 0;
                i_37_ = 1;
                while (idx < chars.length) {
                    char c = chars[idx];
                    char c_41_ = '\0';
                    if (idx + 1 < chars.length) {
                        c_41_ = chars[idx + 1];
                    }

                    int i_42_;
                    if (i_39_ < domain.length && (i_42_ = ChatCensor.matchCharacterAliases(c, domain[i_39_], c_41_)) > 0) {
                        idx += i_42_;
                        i_39_++;
                    } else {
                        if (i_39_ == 0) {
                            break;
                        } else if ((i_42_ = ChatCensor.matchCharacterAliases(c, domain[i_39_ - 1], c_41_)) > 0) {
                            idx += i_42_;

                            if (i_39_ == 1) {
                                i_37_++;
                            }
                        } else {
                            if (i_39_ >= domain.length || !ChatCensor.isNonAlphaNumeric(c)) {
                                break;
                            }
                            idx++;
                        }
                    }
                }

                if (i_39_ >= domain.length) {
                    boolean bool_43_ = false;
                    int i_44_ = ChatCensor.method191(chars, cs_35_, j);
                    int i_45_ = ChatCensor.method192(cs_34_, idx - 1, chars);

                    if (i_44_ > 2 || i_45_ > 2) {
                        bool_43_ = true;
                    }

                    if (bool_43_) {
                        for (int i_46_ = j; i_46_ < idx; i_46_++) {
                            chars[i_46_] = '*';
                        }
                    }
                }
            }
        }
    }

    private static final int method191(final char[] chars, final char[] cs_47_, final int idx) {
        if (idx == 0) {
            return 2;
        }

        for (int i = idx - 1; i >= 0; i--) {
            if (!ChatCensor.isNonAlphaNumeric(chars[i])) {
                break;
            } else if (chars[i] == '@') {
                return 3;
            }
        }

        int starCount = 0;
        for (int i_51_ = idx - 1; i_51_ >= 0; i_51_--) {
            if (!ChatCensor.isNonAlphaNumeric(cs_47_[i_51_])) {
                break;
            } else if (cs_47_[i_51_] == '*') {
                starCount++;
            }
        }

        if (starCount >= 3) {
            return 4;
        } else if (ChatCensor.isNonAlphaNumeric(chars[idx - 1])) {
            return 1;
        }

        return 0;
    }

    private static final int method192(final char[] chars, final int idx, final char[] cs_52_) {
        if (idx + 1 == cs_52_.length) {
            return 2;
        }

        for (int j = idx + 1; j < cs_52_.length; j++) {
            if (!ChatCensor.isNonAlphaNumeric(cs_52_[j])) {
                break;
            } else if (cs_52_[j] == '.' || cs_52_[j] == ',') {
                return 3;
            }
        }

        int starCount = 0;
        for (int j = idx + 1; j < cs_52_.length; j++) {
            final char c = chars[j];
            if (!ChatCensor.isNonAlphaNumeric(c)) {
                break;
            } else if (c == '*') {
                ++starCount;
            }
        }

        if (starCount >= 3) {
            return 4;
        }

        if (ChatCensor.isNonAlphaNumeric(cs_52_[idx + 1])) {
            return 1;
        }

        return 0;
    }

    private static final void method193(final char[] chars) {
        final char[] charsCloneDot = chars.clone();
        final char[] charsCloneSlash = chars.clone();
        final char[] CHARS_DOT = {'d', 'o', 't'};
        final char[] CHARS_SLASH = {'s', 'l', 'a', 's', 'h'};

        ChatCensor.method197(null, charsCloneDot, CHARS_DOT);
        ChatCensor.method197(null, charsCloneSlash, CHARS_SLASH);
        for (int i = 0; i < ChatCensor.topLevelDomains.length; i++) {
            ChatCensor.method194(charsCloneSlash, ChatCensor.topLevelDomains[i], ChatCensor.topLevelDomainsType[i],
                    charsCloneDot, chars);
        }
    }

    private static final void method194(final char[] chars, final char[] tld, final int tldType, final char[] cs_61_,
                                        final char[] cs_62_) {
        if (tld.length <= cs_62_.length) {
            int i_63_;
            for (int i_64_ = 0; i_64_ <= cs_62_.length - tld.length; i_64_ += i_63_) {
                int i_65_ = i_64_;
                int i_66_ = 0;
                i_63_ = 1;

                while (i_65_ < cs_62_.length) {
                    char c = cs_62_[i_65_];
                    char c_68_ = '\0';

                    if (i_65_ + 1 < cs_62_.length) {
                        c_68_ = cs_62_[i_65_ + 1];
                    }

                    int i_69_;
                    if (i_66_ < tld.length && (i_69_ = ChatCensor.matchCharacterAliases(c, tld[i_66_], c_68_)) > 0) {
                        i_65_ += i_69_;
                        ++i_66_;
                    } else {
                        if (i_66_ == 0) {
                            break;
                        }

                        if ((i_69_ = ChatCensor.matchCharacterAliases(c, tld[i_66_ - 1], c_68_)) > 0) {
                            i_65_ += i_69_;

                            if (i_66_ == 1) {
                                ++i_63_;
                            }
                        } else {
                            if (i_66_ >= tld.length || !ChatCensor.isNonAlphaNumeric(c)) {
                                break;
                            }

                            ++i_65_;
                        }
                    }
                }

                if (i_66_ >= tld.length) {
                    boolean bool_70_ = false;
                    int i_71_ = ChatCensor.method195(cs_62_, i_64_, cs_61_);
                    int i_72_ = ChatCensor.method196(cs_62_, chars, i_65_ - 1);

                    if (tldType == 1 && i_71_ > 0 && i_72_ > 0) {
                        bool_70_ = true;
                    }

                    if (tldType == 2 && (i_71_ > 2 && i_72_ > 0 || i_71_ > 0 && i_72_ > 2)) {
                        bool_70_ = true;
                    }

                    if (tldType == 3 && i_71_ > 0 && i_72_ > 2) {
                        bool_70_ = true;
                    }

                    if (tldType == 3 && i_71_ > 2 && i_72_ > 0) {
                        /* empty */
                    }

                    if (bool_70_) {
                        int i_73_ = i_64_;
                        int i_74_ = i_65_ - 1;

                        if (i_71_ > 2) {
                            if (i_71_ == 4) {
                                boolean bool_75_ = false;
                                for (int i_76_ = i_73_ - 1; i_76_ >= 0; i_76_--) {
                                    if (bool_75_) {
                                        if (cs_61_[i_76_] != '*') {
                                            break;
                                        }

                                        i_73_ = i_76_;
                                    } else if (cs_61_[i_76_] == '*') {
                                        i_73_ = i_76_;
                                        bool_75_ = true;
                                    }
                                }
                            }

                            boolean bool_77_ = false;
                            for (int i_78_ = i_73_ - 1; i_78_ >= 0; i_78_--) {
                                if (bool_77_) {
                                    if (ChatCensor.isNonAlphaNumeric(cs_62_[i_78_])) {
                                        break;
                                    }

                                    i_73_ = i_78_;
                                } else if (!ChatCensor.isNonAlphaNumeric(cs_62_[i_78_])) {
                                    bool_77_ = true;
                                    i_73_ = i_78_;
                                }
                            }
                        }

                        if (i_72_ > 2) {
                            if (i_72_ == 4) {
                                boolean bool_79_ = false;
                                for (int i_80_ = i_74_ + 1; i_80_ < cs_62_.length; i_80_++) {
                                    if (bool_79_) {
                                        if (chars[i_80_] != '*') {
                                            break;
                                        }

                                        i_74_ = i_80_;
                                    } else if (chars[i_80_] == '*') {
                                        i_74_ = i_80_;
                                        bool_79_ = true;
                                    }
                                }
                            }

                            boolean bool_81_ = false;
                            for (int i_82_ = i_74_ + 1; i_82_ < cs_62_.length; i_82_++) {
                                if (bool_81_) {
                                    if (ChatCensor.isNonAlphaNumeric(cs_62_[i_82_])) {
                                        break;
                                    }

                                    i_74_ = i_82_;
                                } else if (!ChatCensor.isNonAlphaNumeric(cs_62_[i_82_])) {
                                    bool_81_ = true;
                                    i_74_ = i_82_;
                                }
                            }
                        }

                        for (int i_83_ = i_73_; i_83_ <= i_74_; i_83_++) {
                            cs_62_[i_83_] = '*';
                        }
                    }
                }
            }
        }
    }

    private static final int method195(final char[] cs, final int i_84_, final char[] cs_85_) {
        if (i_84_ == 0) {
            return 2;
        }

        for (int i = i_84_ - 1; i >= 0; i--) {
            if (!ChatCensor.isNonAlphaNumeric(cs[i])) {
                break;
            } else if (cs[i] == ',' || cs[i] == '.') {
                return 3;
            }
        }

        int starCount = 0;
        for (int i = i_84_ - 1; i >= 0; i--) {
            if (!ChatCensor.isNonAlphaNumeric(cs_85_[i])) {
                break;
            } else if (cs_85_[i] == '*') {
                starCount++;
            }
        }

        if (starCount >= 3) {
            return 4;
        } else if (ChatCensor.isNonAlphaNumeric(cs[i_84_ - 1])) {
            return 1;
        }

        return 0;
    }

    private static final int method196(final char[] chars, final char[] cs_89_, final int i) {
        if (i + 1 == chars.length) {
            return 2;
        }

        for (int idx = i + 1; idx < chars.length; idx++) {
            if (!ChatCensor.isNonAlphaNumeric(chars[idx])) {
                break;
            } else if (chars[idx] == '\\' || chars[idx] == '/') {
                return 3;
            }
        }

        int starCount = 0;
        for (int j = i + 1; j < chars.length; j++) {
            if (!ChatCensor.isNonAlphaNumeric(cs_89_[j])) {
                break;
            } else if (cs_89_[j] == '*') {
                starCount++;
            }
        }

        if (starCount >= 5) {
            return 4;
        }

        if (ChatCensor.isNonAlphaNumeric(chars[i + 1])) {
            return 1;
        }

        return 0;
    }

    public static final void method197(final byte[][] _badBytes, final char[] chars, final char[] badWord) {
        if (badWord.length > chars.length) {
            return;
        }

        int i_95_;
        for (int j = 0; j <= chars.length - badWord.length; j += i_95_) {
            i_95_ = 1;

            int idx = j;
            int m = 0;
            int n = 0;

            boolean flag_a = false;
            boolean flag_b = false;
            boolean flab_c = false;

            while (idx < chars.length && (!flag_b || !flab_c)) {
                final char c = chars[idx];
                char c_103_ = '\0';

                if (idx + 1 < chars.length) {
                    c_103_ = chars[idx + 1];
                }

                int i_104_ = ChatCensor.checkLetter(c_103_, c, badWord[m]);
                if (m < badWord.length && i_104_ > 0) {
                    if (i_104_ == 1 && ChatCensor.isDigit(c)) {
                        flag_b = true;
                    } else if (i_104_ == 2 && (ChatCensor.isDigit(c) || ChatCensor.isDigit(c_103_))) {
                        flag_b = true;
                    }

                    idx += i_104_;
                    ++m;
                } else {
                    if (m == 0) {
                        break;
                    }

                    if ((i_104_ = ChatCensor.checkLetter(c_103_, c, badWord[m - 1])) > 0) {
                        idx += i_104_;

                        if (m == 1) {
                            ++i_95_;
                        }
                    } else {
                        if (m >= badWord.length || !ChatCensor.matchesCharacterCheck(c)) {
                            break;
                        }

                        if (ChatCensor.isNonAlphaNumeric(c) && c != '\'') {
                            flag_a = true;
                        }

                        if (ChatCensor.isDigit(c)) {
                            flab_c = true;
                        }

                        ++idx;

                        if (++n * 100 / (idx - j) > 90) {
                            break;
                        }
                    }
                }
            }

            if (m >= badWord.length && (!flag_b || !flab_c)) {
                boolean flag_d = true;

                if (!flag_a) {
                    char c = ' ';
                    if (j - 1 >= 0) {
                        c = chars[j - 1];
                    }

                    char idxC = ' ';
                    if (idx < chars.length) {
                        idxC = chars[idx];
                    }

                    byte packedC = ChatCensor.charTo5Bit(c);
                    byte packedIdxC = ChatCensor.charTo5Bit(idxC);
                    if (_badBytes != null && ChatCensor.method198(packedC, _badBytes, packedIdxC)) {
                        flag_d = false;
                    }
                } else {
                    final boolean nonAlpha = (j - 1 < 0 || ChatCensor.isNonAlphaNumeric(chars[j - 1]) && chars[j - 1] != '\'');
                    final boolean bool_109_ = (idx >= chars.length || ChatCensor.isNonAlphaNumeric(chars[idx]) && chars[idx] != '\'');

                    if (!nonAlpha || !bool_109_) {
                        boolean notHasFragments = false;
                        int fromEnd = j - 2;

                        if (nonAlpha) {
                            fromEnd = j;
                        }

                        for (; !notHasFragments && fromEnd < idx; fromEnd++) {
                            final char c = chars[fromEnd];
                            if (fromEnd >= 0 && (!ChatCensor.isNonAlphaNumeric(c) || c == '\'')) {
                                int i_113_;
                                final char[] threeChars = new char[3];
                                for (i_113_ = 0; i_113_ < 3; i_113_++) {
                                    if (fromEnd + i_113_ >= chars.length
                                            || ChatCensor.isNonAlphaNumeric(chars[fromEnd + i_113_])
                                            && chars[fromEnd + i_113_] != '\'') {
                                        break;
                                    }

                                    threeChars[i_113_] = chars[fromEnd + i_113_];
                                }

                                boolean bool_114_ = i_113_ == 0;

                                if (i_113_ < 3 && fromEnd - 1 >= 0
                                        && (!ChatCensor.isNonAlphaNumeric(chars[fromEnd - 1]) || chars[fromEnd - 1] == '\'')) {
                                    bool_114_ = false;
                                }

                                if (bool_114_ && !ChatCensor.handleFragments(threeChars)) {
                                    notHasFragments = true;
                                }
                            }
                        }

                        if (!notHasFragments) {
                            flag_d = false;
                        }
                    }
                }

                if (flag_d) {
                    int digitCount = 0;
                    int letterCount = 0;
                    int lastLetterIndex = -1;

                    for (int i = j; i < idx; i++) {
                        if (ChatCensor.isDigit(chars[i])) {
                            digitCount++;
                        } else if (ChatCensor.isLetter(chars[i])) {
                            letterCount++;
                            lastLetterIndex = i;
                        }
                    }

                    if (lastLetterIndex > -1) {
                        digitCount -= idx - 1 - lastLetterIndex;
                    }

                    if (digitCount <= letterCount) {
                        for (int k = j; k < idx; k++) {
                            chars[k] = '*';
                        }
                    } else {
                        i_95_ = 1;
                    }
                }
            }
        }
    }

    private static final boolean method198(final byte smallChar, final byte[][] _badBytes, final byte current) {
        int i = 0;
        if (_badBytes[i][0] == smallChar && _badBytes[i][1] == current) {
            return true;
        }

        int j = _badBytes.length - 1;
        if (_badBytes[j][0] == smallChar && _badBytes[j][1] == current) {
            return true;
        }

        do {
            int k = (i + j) / 2;
            if (_badBytes[k][0] == smallChar && _badBytes[k][1] == current) {
                return true;
            } else if (smallChar < _badBytes[k][0] || smallChar == _badBytes[k][0] && current < _badBytes[k][1]) {
                j = k;
            } else {
                i = k;
            }
        } while (i != j && i + 1 != j);

        return false;
    }

    /**
     * @param first
     * @param suspect
     * @param second
     * @return number of characters matched
     */
    private static final int matchCharacterAliases(final char first, final char suspect, final char second) {
        if (suspect == first) {
            return 1;
        } else if (suspect == 'o' && first == '0') {
            return 1;
        } else if (suspect == 'o' && first == '(' && second == ')') {
            return 2;
        } else if (suspect == 'c' && (first == '(' || first == '<' || first == '[')) {
            return 1;
        } else if (suspect == 'e' && first == '\u20ac') {
            return 1;
        } else if (suspect == 's' && first == '$') {
            return 1;
        } else if (suspect == 'l' && first == 'i') {
            return 1;
        }

        return 0;
    }

    /**
     * @param second
     * @param first
     * @param aliasee
     * @return number of letters consumed
     */
    private static final int checkLetter(final char second, final char first, final char aliasee) {
        if (aliasee == first) {
            return 1;
        }

        if (isLetter(aliasee)) {
            if (aliasee == 'a') {
                if (first == '4' || first == '@' || first == '^') {
                    return 1;
                } else if (first == '/' && second == '\\') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'b') {
                if (first == '6' || first == '8') {
                    return 1;
                } else if (first == '1' && second == '3' || first == 'i' && second == '3') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'c') {
                if (first == '(' || first == '<' || first == '{' || first == '[') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 'd') {
                if (first == '[' && second == ')' || first == 'i' && second == ')') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'e') {
                if (first == '3' || first == '\u20ac') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 'f') {
                if (first == 'p' && second == 'h') {
                    return 2;
                } else if (first == '\u00a3') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 'g') {
                if (first == '9' || first == '6' || first == 'q') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 'h') {
                if (first == '#') {
                    return 1;
                }

                return 0;
            }
            if (aliasee == 'i') {
                if (first == 'y' || first == 'l' || first == 'j' || first == '1' || first == '!'
                        || first == ':' || first == ';' || first == '|') {
                    return 1;
                }
                return 0;
            } else if (aliasee == 'j') {
                return 0;
            } else if (aliasee == 'k') {
                return 0;
            } else if (aliasee == 'l') {
                if (first == '1' || first == '|' || first == 'i') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 'm') {
                return 0;
            } else if (aliasee == 'n') {
                return 0;
            } else if (aliasee == 'o') {
                if (first == '0' || first == '*') {
                    return 1;
                }

                if (first == '(' && second == ')' || first == '[' && second == ']' || first == '{' && second == '}'
                        || first == '<' && second == '>') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'p') {
                return 0;
            } else if (aliasee == 'q') {
                return 0;
            } else if (aliasee == 'r') {
                return 0;
            } else if (aliasee == 's') {
                if (first == '5' || first == 'z' || first == '$' || first == '2') {
                    return 1;
                }

                return 0;
            } else if (aliasee == 't') {
                if (first == '7' || first == '+') {
                    return 1;
                }
                return 0;
            } else if (aliasee == 'u') {
                if (first == 'v') {
                    return 1;
                }

                if (first == '\\' && second == '/' || first == '\\' && second == '|' || first == '|' && second == '/') {
                    return 2;
                }
                return 0;
            } else if (aliasee == 'v') {
                if (first == '\\' && second == '/' || first == '\\' && second == '|' || first == '|' && second == '/') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'w') {
                if (first == 'v' && second == 'v') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'x') {
                if (first == ')' && second == '(' || first == '}' && second == '{' || first == ']' && second == '['
                        || first == '>' && second == '<') {
                    return 2;
                }

                return 0;
            } else if (aliasee == 'y') {
                return 0;
            } else if (aliasee == 'z') {
                return 0;
            }
        }

        if (isDigit(aliasee)) {
            if (aliasee == '0') {
                if (first == 'o' || first == 'O') {
                    return 1;
                }

                if (first == '(' && second == ')' || first == '{' && second == '}' || first == '[' && second == ']') {
                    return 2;
                }

                return 0;
            }

            if (aliasee == '1') {
                if (first == 'l') {
                    return 1;
                }

                return 0;
            }

            return 0;
        }

        if (aliasee == ',') {
            if (first == '.') {
                return 1;
            }

            return 0;
        }

        if (aliasee == '.') {
            if (first == ',') {
                return 1;
            }

            return 0;
        }

        if (aliasee == '!') {
            if (first == 'i') {
                return 1;
            }

            return 0;
        }

        return 0;
    }

    private static final byte charTo5Bit(final char c) {
        if (c >= 'a' && c <= 'z') {
            return (byte) (c - 'a' + '\001');
        } else if (c == '\'') {
            return (byte) 28;
        } else if (c >= '0' && c <= '9') {
            return (byte) (c - '0' + '\035');
        }

        // default is space
        return (byte) 27;
    }

    private static final void method202(final char[] chars) {
        int i = 0;
        int i_129_ = 0;
        int startCensor = 0;

        int digitIndex;
        while ((digitIndex = ChatCensor.getFirstDigitIndex(chars, i)) != -1) {
            boolean bool_132_ = false;
            for (int j = i; j >= 0 && j < digitIndex; j++) {
                final char c = chars[j];
                if (!ChatCensor.isNonAlphaNumeric(c) && !ChatCensor.matchesCharacterCheck(c)) {
                    bool_132_ = true;
                    break;
                }
            }

            if (bool_132_) {
                i_129_ = 0;
            }

            if (i_129_ == 0) {
                startCensor = digitIndex;
            }

            int unpacked = 0;
            i = ChatCensor.getFirstNonDigitIndex(chars, digitIndex);
            for (int j = digitIndex; j < i; j++) {
                unpacked = unpacked * 10 + chars[j] - 48;
            }

            if (unpacked > 255 || i - digitIndex > 8) {
                i_129_ = 0;
            } else {
                ++i_129_;
            }

            if (i_129_ == 4) {
                for (int j = startCensor; j < i; j++) {
                    chars[j] = '*';
                }

                i_129_ = 0;
            }
        }
    }

    private static final int getFirstDigitIndex(final char[] chars, final int startIndex) {
        for (int idx = startIndex; idx < chars.length && idx >= 0; idx++) {
            if (isDigit(chars[idx])) {
                return idx;
            }
        }

        return -1;
    }

    private static final int getFirstNonDigitIndex(final char[] chars, final int startIndex) {
        for (int idx = startIndex; idx < chars.length && idx >= 0; idx++) {
            if (!isDigit(chars[idx])) {
                return idx;
            }
        }

        return chars.length;
    }

    private static final boolean isNonAlphaNumeric(final char c) {
        return !ChatCensor.isLetter(c) && !ChatCensor.isDigit(c);
    }

    private static final boolean matchesCharacterCheck(final char c) {
        if (c < 'a' || c > 'z') {
            return true;
        }

        return (c == 'v' || c == 'x' || c == 'j' || c == 'q' || c == 'z');
    }

    private static final boolean isLetter(final char c) {
        return isLowerCase(c) || isUpperCase(c);
    }

    private static final boolean isDigit(final char c) {
        return (c >= '0' && c <= '9');
    }

    private static final boolean isLowerCase(final char c) {
        return (c >= 'a' || c <= 'z');
    }

    private static final boolean isUpperCase(final char c) {
        return (c >= 'A' || c <= 'Z');
    }

    private static final boolean handleFragments(final char[] chars) {
        boolean numeric = true;
        for (int i = 0; i < chars.length; i++) {
            if (!ChatCensor.isDigit(chars[i]) && chars[i] != '\0') {
                numeric = false;
                break;
            }
        }

        if (numeric) {
            return true;
        }

        int i = 0;
        final int packed = ChatCensor.packString(chars);
        int lastFragmentIndex = ChatCensor.fragments.length - 1;

        if (packed == ChatCensor.fragments[i] || packed == ChatCensor.fragments[lastFragmentIndex]) {
            return true;
        }

        do {
            final int k = (i + lastFragmentIndex) / 2;
            if (packed == ChatCensor.fragments[k]) {
                return true;
            } else if (packed < ChatCensor.fragments[k]) {
                lastFragmentIndex = k;
            } else {
                i = k;
            }
        } while (i != lastFragmentIndex && i + 1 != lastFragmentIndex);

        return false;
    }

    public static final int packString(final char[] chars) {
        if (chars.length > 6) {
            return 0;
        }

        int packed = 0;
        for (int j = 0; j < chars.length; j++) {
            final char c = chars[chars.length - j - 1];
            if (c >= 'a' && c <= 'z') {
                packed = packed * 38 + (c - 'a' + 1);
            } else if (c == '\'') {
                packed = packed * 38 + 27;
            } else if (c >= '0' && c <= '9') {
                packed = packed * 38 + (c - '0' + 28);
            } else if (c != '\0') {
                return 0;
            }
        }

        return packed;
    }
}
