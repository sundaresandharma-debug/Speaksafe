// SpeakSafe built-in model (JavaScript version of the Java detection engine).
//
// The Java backend exports its trained Linear SVM to model.json
// (java -jar speaksafe.jar --export-model model.json). This file repeats the
// same steps in the browser, so the extension and the web demo work without the server:
//   clean text -> words -> word stems -> TF-IDF -> SVM -> score 0-100 -> category -> action
// Every step mirrors a Java class (named in the comments) so both give the same scores.

(function (root) {
  'use strict';

  // ---------------------------------------------------------------
  // Porter stemmer: a line-by-line port of opennlp.tools.stemmer.PorterStemmer
  // ---------------------------------------------------------------
  function porterStem(word) {
    const b = word.split('');
    let j = 0, k = b.length - 1, dirty = false;
    const k0 = 0;

    const cons = i => {
      switch (b[i]) {
        case 'a': case 'e': case 'i': case 'o': case 'u': return false;
        case 'y': return i === k0 || !cons(i - 1);
        default: return true;
      }
    };
    const m = () => {
      let n = 0, i = k0;
      while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
      i++;
      while (true) {
        while (true) { if (i > j) return n; if (cons(i)) break; i++; }
        i++; n++;
        while (true) { if (i > j) return n; if (!cons(i)) break; i++; }
        i++;
      }
    };
    const vowelInStem = () => { for (let i = k0; i <= j; i++) if (!cons(i)) return true; return false; };
    const doublec = jj => jj >= k0 + 1 && b[jj] === b[jj - 1] && cons(jj);
    const cvc = i => {
      if (i < k0 + 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
      const ch = b[i];
      return !(ch === 'w' || ch === 'x' || ch === 'y');
    };
    const ends = s => {
      const l = s.length, o = k - l + 1;
      if (o < k0) return false;
      for (let i = 0; i < l; i++) if (b[o + i] !== s[i]) return false;
      j = k - l;
      return true;
    };
    const setto = s => {
      for (let i = 0; i < s.length; i++) b[j + 1 + i] = s[i];
      k = j + s.length;
      dirty = true;
    };
    const r = s => { if (m() > 0) setto(s); };

    function step1() {
      if (b[k] === 's') {
        if (ends('sses')) k -= 2;
        else if (ends('ies')) setto('i');
        else if (b[k - 1] !== 's') k--;
      }
      if (ends('eed')) {
        if (m() > 0) k--;
      } else if ((ends('ed') || ends('ing')) && vowelInStem()) {
        k = j;
        if (ends('at')) setto('ate');
        else if (ends('bl')) setto('ble');
        else if (ends('iz')) setto('ize');
        else if (doublec(k)) {
          const ch = b[k--];
          if (ch === 'l' || ch === 's' || ch === 'z') k++;
        } else if (m() === 1 && cvc(k)) setto('e');
      }
    }
    function step2() {
      if (ends('y') && vowelInStem()) { b[k] = 'i'; dirty = true; }
    }
    function tryRules(rules) {
      for (const [suffix, replacement] of rules) {
        if (ends(suffix)) { r(replacement); return; }
      }
    }
    function step3() {
      if (k === k0) return;
      const rules = {
        a: [['ational', 'ate'], ['tional', 'tion']],
        c: [['enci', 'ence'], ['anci', 'ance']],
        e: [['izer', 'ize']],
        l: [['bli', 'ble'], ['alli', 'al'], ['entli', 'ent'], ['eli', 'e'], ['ousli', 'ous']],
        o: [['ization', 'ize'], ['ation', 'ate'], ['ator', 'ate']],
        s: [['alism', 'al'], ['iveness', 'ive'], ['fulness', 'ful'], ['ousness', 'ous']],
        t: [['aliti', 'al'], ['iviti', 'ive'], ['biliti', 'ble']],
        g: [['logi', 'log']]
      }[b[k - 1]];
      if (rules) tryRules(rules);
    }
    function step4() {
      const rules = {
        e: [['icate', 'ic'], ['ative', ''], ['alize', 'al']],
        i: [['iciti', 'ic']],
        l: [['ical', 'ic'], ['ful', '']],
        s: [['ness', '']]
      }[b[k]];
      if (rules) tryRules(rules);
    }
    function step5() {
      if (k === k0) return;
      let found = false;
      switch (b[k - 1]) {
        case 'a': found = ends('al'); break;
        case 'c': found = ends('ance') || ends('ence'); break;
        case 'e': found = ends('er'); break;
        case 'i': found = ends('ic'); break;
        case 'l': found = ends('able') || ends('ible'); break;
        case 'n': found = ends('ant') || ends('ement') || ends('ment') || ends('ent'); break;
        case 'o': found = (ends('ion') && j >= 0 && (b[j] === 's' || b[j] === 't')) || ends('ou'); break;
        case 's': found = ends('ism'); break;
        case 't': found = ends('ate') || ends('iti'); break;
        case 'u': found = ends('ous'); break;
        case 'v': found = ends('ive'); break;
        case 'z': found = ends('ize'); break;
      }
      if (found && m() > 1) k = j;
    }
    function step6() {
      j = k;
      if (b[k] === 'e') {
        const a = m();
        if (a > 1 || (a === 1 && !cvc(k - 1))) k--;
      }
      if (b[k] === 'l' && doublec(k) && m() > 1) k--;
    }

    const length = b.length;
    if (k > k0 + 1) { step1(); step2(); step3(); step4(); step5(); step6(); }
    if (length !== k + 1) dirty = true;
    return dirty ? b.slice(0, k + 1).join('') : word;
  }

  // ---------------------------------------------------------------
  // Preprocessing (same as TextPreprocessor.java)
  // ---------------------------------------------------------------
  const JAVA_SPACE = '[ \\t\\n\\x0B\\f\\r]';   // Java's \s (ASCII whitespace only)

  function expandContractions(text) {
    return text.replace(/\u2019/g, "'")
      .replace(/\bcan't\b/g, 'can not')
      .replace(/\bwon't\b/g, 'will not')
      .replace(/n't\b/g, ' not')
      .replace(/'re\b/g, ' are')
      .replace(/'ll\b/g, ' will')
      .replace(/'m\b/g, ' am')
      .replace(/'ve\b/g, ' have')
      .replace(/'d\b/g, ' would')
      .replace(/'s\b/g, '');
  }

  function normalizeLeetspeak(text) {
    const words = text.split(new RegExp(JAVA_SPACE + '+'));
    while (words.length && words[words.length - 1] === '') words.pop();   // Java's split() drops trailing empties
    let result = '';
    for (let word of words) {
      if (/[a-z]/.test(word)) {
        word = word.replace(/0/g, 'o').replace(/1/g, 'i').replace(/3/g, 'e')
                   .replace(/4/g, 'a').replace(/5/g, 's').replace(/7/g, 't')
                   .replace(/@/g, 'a').replace(/\$/g, 's')
                   .replace(/\*/g, '');                                       // f*ck -> fck
      }
      result += word + ' ';
    }
    return result;
  }

  function preprocess(text, stopwords, slang) {
    slang = slang || {};
    if (text == null) return [];
    let clean = String(text).toLowerCase();
    clean = clean.replace(/https?:\/\/[^ \t\n\x0B\f\r]+|www\.[^ \t\n\x0B\f\r]+/g, ' ');   // remove links
    clean = clean.replace(/@[a-zA-Z0-9_]+/g, ' ');                                       // remove @mentions
    clean = expandContractions(clean);                                                  // you're -> you are
    clean = normalizeLeetspeak(clean);                                                  // 1d10t -> idiot
    clean = clean.replace(/([^\n\r\u0085\u2028\u2029])\1{2,}/gu, '$1');                 // stuuupid -> stupid
    clean = clean.replace(/[^a-z \t\n\x0B\f\r]/g, ' ');                                 // remove symbols and digits
    const tokens = [];
    for (let token of clean.split(new RegExp(JAVA_SPACE + '+'))) {
      if (Object.prototype.hasOwnProperty.call(slang, token)) token = slang[token];   // f*ck / fck -> fuck
      if (token.length > 1 && !stopwords.has(token)) tokens.push(porterStem(token));
    }
    return tokens;
  }

  // ---------------------------------------------------------------
  // The model: TF-IDF (TfIdfVectorizer.java) + Linear SVM (SvmClassifier.java)
  // + scoring, category and action (ToxicityDetector.java)
  // ---------------------------------------------------------------
  class SpeakSafeModel {
    constructor(json) {
      this.name = json.name;
      this.categories = json.categories;               // [SAFE, INSULT, THREAT, HATE_SPEECH]
      this.stopwords = new Set(json.stopwords);
      this.slang = json.slang || {};
      this.vocabulary = new Map(json.terms.map((term, i) => [term, i]));
      this.idf = json.idf;
      this.weights = json.weights;
      this.bias = json.bias;
      this.warnThreshold = json.warnThreshold;
      this.blurThreshold = json.blurThreshold;
    }

    vectorize(words) {
      const terms = words.slice();
      for (let i = 0; i + 1 < words.length; i++) terms.push(words[i] + '_' + words[i + 1]);
      const vector = new Map();
      for (const term of terms) {
        const index = this.vocabulary.get(term);
        if (index !== undefined) vector.set(index, (vector.get(index) || 0) + 1);
      }
      let length = 0;
      for (const [index, tf] of vector) {
        const weight = tf * this.idf[index];
        vector.set(index, weight);
        length += weight * weight;
      }
      const norm = Math.sqrt(length);
      if (norm > 0) for (const [index, weight] of vector) vector.set(index, weight / norm);
      return vector;
    }

    probabilities(vector) {
      const probabilities = {};
      if (vector.size === 0) {   // no known words -> safe
        this.categories.forEach(c => { probabilities[c] = c === 'SAFE' ? 1 : 0; });
        return probabilities;
      }
      const p = this.categories.map((c, ci) => {
        let score = this.bias[ci];
        for (const [index, value] of vector) score += this.weights[ci][index] * value;
        return 1 / (1 + Math.exp(-3 * score));
      });
      let sum = p.reduce((a, x) => a + x, 0);
      const safe = this.categories.indexOf('SAFE');
      if (sum < 1) { p[safe] += 1 - sum; sum = 1; }
      this.categories.forEach((c, ci) => { probabilities[c] = p[ci] / sum; });
      return probabilities;
    }

    analyze(text) {
      text = String(text == null ? '' : text).slice(0, 5000);
      const probabilities = this.probabilities(this.vectorize(preprocess(text, this.stopwords, this.slang)));
      const score = Math.round((1 - probabilities.SAFE) * 100);
      let category = 'SAFE';
      if (score >= this.warnThreshold) {
        let best = -1;
        for (const c of this.categories) {
          if (c !== 'SAFE' && probabilities[c] > best) { best = probabilities[c]; category = c; }
        }
      }
      const action = category === 'SAFE' ? 'NONE' : score >= this.blurThreshold ? 'BLUR' : 'WARN';
      return { text, score, category, action, probabilities, model: 'Linear SVM (built-in)' };
    }
  }

  SpeakSafeModel.preprocess = preprocess;
  SpeakSafeModel.porterStem = porterStem;
  root.SpeakSafeModel = SpeakSafeModel;
})(typeof self !== 'undefined' ? self : globalThis);
