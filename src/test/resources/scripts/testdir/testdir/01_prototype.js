/*
 * Copyright (c) tocata GmbH. All rights reserved.
 *
 * @version 1.0	(19.07.2018)
 */
/* - - - - - - - - - - - JavaScript extensions - - - - - - - - - - - */

// With thanks to https://developer.mozilla.org/En/Core_JavaScript_1.5_Reference:Objects:Array:forEach#Compatibility
// Usage: anArray.each(function(index, anArray[index], anArray) { ... }); -- Answer false to break the loop.
if (! Array.prototype.each) {
//	debugLogMessage("Creating Array.prototype.each().");
	Array.prototype.each = function(pCallback) {
		var len = this.length >>> 0,
		thisp = arguments[1];
//		if (typeof pCallback !== "function") {	IE answers "object" ... but does the job, so skip the check here
//			throw new TypeError();
//		}
		for (var index = 0; index < len; index++) {
			if (index in this) {
				if (pCallback.call(thisp, index, this[index], this) === false) {
					break;
				}
			}
		}
	};
}

if (! String.prototype.endsWith) {	// ECMA6 provides it, e.g. Gecko
//	debugLogMessage("Creating String.prototype.endsWith().");
	String.prototype.endsWith = function(pSuffix) {
		return pSuffix
			&& (0 < pSuffix.length)
			&& (pSuffix.length <= this.length)
			&& this.substring(this.length - pSuffix.length) == pSuffix;
	};
}

if (! String.prototype.startsWith) {
//	debugLogMessage("Creating String.prototype.startsWith().");
	String.prototype.startsWith = function(pSearchString, pPosition) {
		var position = pPosition || 0;
		return pSearchString
			&& (position < pSearchString.length)
			&& this.substring(position, position + pSearchString.length) == pSearchString;
	};
}

// Thanks to http://cwestblog.com/2011/10/11/javascript-snippet-string-prototype-hashcode/
// The number returned will always be in the -2^31 to 2^31-1 range.
if (! String.prototype.hashCode) {
//	debugLogMessage("Creating String.prototype.hashCode().");
	String.prototype.hashCode = function() {
		var result = 0,
			len = this.length;
		for (var index = 0; index < len; index++) {
			result = (31 * result + this.charCodeAt(index)) << 0;
		}
		return result;
	};
}