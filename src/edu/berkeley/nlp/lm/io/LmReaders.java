package edu.berkeley.nlp.lm.io;

import edu.berkeley.nlp.lm.KatzBackoffLm;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.array.LongArray;
import edu.berkeley.nlp.lm.map.HashNgramMap;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.map.NgramMapOpts;
import edu.berkeley.nlp.lm.util.hash.MurmurHash;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;
import edu.berkeley.nlp.lm.values.ProbBackoffValueContainer;

public class LmReaders
{

	/**
	 * Factory method for reading an ARPA lm file.
	 * @param <W>
	 * @param opts
	 * @param lmFile
	 * @param lmOrder
	 * @param wordIndexer
	 * @return
	 */
	public static <W> KatzBackoffLm<W> readArpaLmFile(final NgramMapOpts opts, final String lmFile, final int lmOrder, final WordIndexer<W> wordIndexer) {

		final FirstPassCallback<ProbBackoffPair> valueAddingCallback = firstPass(opts, lmFile, lmOrder, wordIndexer);
		final LongArray[] numNgramsForEachWord = valueAddingCallback.getNumNgramsForEachWord();
		return secondPass(opts, lmFile, lmOrder, wordIndexer, valueAddingCallback, numNgramsForEachWord);
	}

	/**
	 * Second pass actually builds the lm.
	 * @param <W>
	 * @param opts
	 * @param lmFile
	 * @param lmOrder
	 * @param wordIndexer
	 * @param valueAddingCallback
	 * @param numNgramsForEachWord
	 * @return
	 */
	private static <W> KatzBackoffLm<W> secondPass(final NgramMapOpts opts, final String lmFile, final int lmOrder, final WordIndexer<W> wordIndexer,
		final FirstPassCallback<ProbBackoffPair> valueAddingCallback, final LongArray[] numNgramsForEachWord) {
		final ProbBackoffValueContainer values = new ProbBackoffValueContainer(valueAddingCallback.getIndexer(), opts.valueRadix, opts.storePrefixIndexes);
		final NgramMap<ProbBackoffPair> map = new HashNgramMap<ProbBackoffPair>(values, new MurmurHash(), opts, numNgramsForEachWord);
		final ARPALmReader<W> arpaLmReader = new ARPALmReader<W>(lmFile, wordIndexer, lmOrder);
		arpaLmReader.parse(new NgramMapAddingCallback<ProbBackoffPair>(map));
		wordIndexer.trimAndLock();

		return new KatzBackoffLm<W>(lmOrder, wordIndexer, map, opts);
	}

	/**
	 * First pass over the file collects some statistics which help with memory
	 * allocation
	 * 
	 * @param <W>
	 * @param opts
	 * @param lmFile
	 * @param lmOrder
	 * @param wordIndexer
	 * @return
	 */
	private static <W> FirstPassCallback<ProbBackoffPair> firstPass(final NgramMapOpts opts, final String lmFile, final int lmOrder,
		final WordIndexer<W> wordIndexer) {
		final ARPALmReader<W> arpaLmReader = new ARPALmReader<W>(lmFile, wordIndexer, lmOrder);
		final FirstPassCallback<ProbBackoffPair> valueAddingCallback = new FirstPassCallback<ProbBackoffPair>(opts);
		arpaLmReader.parse(valueAddingCallback);
		return valueAddingCallback;
	}

}
