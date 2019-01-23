/*
 * Common functions
 */
function rotate(page) {
  switch (page.rotate) {
    case 0:
      return '';
    case 1:
      return '-rotate 90';
    case 2:
      return '-rotate 180';
    case 3:
      return '-rotate 270';
    default:
      throw "Unknown rotate: " + page.rotate;
  }
}
function scale(book) {
  if (book.scale != 0) {
    return '-adaptive-resize '+book.scale + '%';
  } else {
    return '';
  }
}
function dcrawc(page) {
  return page.tags.color ? '' : '-d';
}
function dpi(book) {
  return '-dpi '+Math.round(book.dpi*book.scale/100);
}

function command_preview() {
  cmd.exec('${settings.path_convert} ${page.originalPageFile} -scale ${settings.preview_size}x${settings.preview_size} preview/${page.number}.jpg');
}

/*
 * 'edit' processing
 */
function exist_edit() {
  if (page.tags.foredit) {
    return cmd.fileExist(page.number+'-edit.png');
  } else {
    return true;
  }
}
function execute_edit() {
  if (page.tags.foredit) {
    cmd.exec('${settings.path_dcraw} ${settings.dcraw_params} -P ../${page.camera}.bad -c ${page.number}.raw | '+
             '${settings.path_convert} - -crop ${book.cropSizeX}x${book.cropSizeY}+${page.cropPosX}+${page.cropPosY} '+rotate(page)+' '+scale(book)+' -strip ${page.number}-edit.png');
  }
}
function bookexist_edit() {
  return true;
}

/*
 * 'djvu' processing
 */
function exist_djvu() {
  return cmd.fileExist(page.number+'.djvu');
}
function execute_djvu() {
  if (page.tags.foredit) {
    cmd.exec('${settings.path_convert} ${page.number}-edit.png ${page.number}-temp.pnm');
  } else {
    cmd.exec('${settings.path_dcraw} ${settings.dcraw_params} -P ../${page.camera}.bad -c ${page.number}.raw | '+
        '${settings.path_convert} - -crop ${book.cropSizeX}x${book.cropSizeY}+${page.cropPosX}+${page.cropPosY} '+rotate(page)+' '+scale(book)+' -strip ${page.number}-temp.pnm');
  }
  cmd.exec('${settings.path_c44} '+dpi(book)+' ${page.number}-temp.pnm ${page.number}.djvu');
  cmd.fileRemove(page.number+'-temp.pnm');
}
function bookexist_djvu() {
  return cmd.fileExist('../#out/book-'+book.name+'.djvu');
}
function bookexecute_djvu() {
  cmd.exec('${settings.path_djvm} -c ../#out/book-'+book.name+'.djvu *.djvu');
  for(p in book.pages) {
    cmd.fileRemove(book.pages[p]+'.djvu');
  }
}

/*
 * 'jp2' processing
 */
function exist_jp2() {
  return cmd.fileExist(page.number+'.jp2');
}
function execute_jp2() {
  if (page.tags.foredit) {
    cmd.exec('${settings.path_convert} ${page.number}-edit.png -quality 75 ${page.number}.jp2');
  } else {
    cmd.exec('${settings.path_dcraw} ${settings.dcraw_params} -P ../${page.camera}.bad -c ${page.number}.raw | '+
             '${settings.path_convert} - -crop ${book.cropSizeX}x${book.cropSizeY}+${page.cropPosX}+${page.cropPosY} '+rotate(page)+' '+scale(book)+' -strip -quality 75 ${page.number}.jp2');
  }
}
function bookexist_jp2() {
  return true;
}

/*
 * 'jpegoverview' processing
 */
function exist_jpegoverview() {
  return cmd.fileExist(page.number+'.jpeg');
}
function execute_jpegoverview() {
    cmd.exec('${settings.path_dcraw} ${settings.dcraw_params} -P ../${page.camera}.bad -c ${page.number}.raw | '+
             '${settings.path_convert} - -crop ${book.cropSizeX}x${book.cropSizeY}+${page.cropPosX}+${page.cropPosY} '+rotate(page)+' -adaptive-resize 50% -strip -quality 85 ${page.number}.jpeg');
}
function bookexist_jpegoverview() {
  return cmd.fileExist('../#out/book-'+book.name+'.zip');
}
function bookexecute_jpegoverview() {
  cmd.exec('7za a -mx=0 ../#out/book-'+book.name+'.zip *.jpeg');
  for(p in book.pages) {
    cmd.fileRemove(book.pages[p]+'.jpeg');
  }
}

function exist_pdf() {
  return cmd.fileExist('temp/'+page.number+'.jp2');
}
function execute_pdf() {
  var levels = page.tags.level_50_100 ? " -level 50%,100% ": "";
  cmd.exec('${settings.path_convert} ${page.originalPageFile} -crop ${book.cropSizeX}x${book.cropSizeY}+${page.cropPosX}+${page.cropPosY} '+rotate(page)+' '+scale(book)+levels+' -strip temp/${page.number}.pnm');
  cmd.exec('${settings.path_opj} -i temp/${page.number}.pnm -q ${settings.jp2_quality} -o temp/${page.number}.jp2');
}
function bookexist_pdf() {
  return cmd.fileExist('../'+book.name+'.pdf');
}
function bookexecute_pdf() {
  cmd.pdf('../'+book.name+'.pdf', 'temp/', 'jp2');
  for(p in book.pages) {
    cmd.fileRemove(book.pages[p]+'.jp2');
  }
}

function exist_pdfnocrop() {
  return exist_pdf();
}
function execute_pdfnocrop() {
  var levels = page.tags.level_50_100 ? " -level 50%,100% ": "";
  cmd.exec('${settings.path_convert} ${page.originalPageFile} '+rotate(page)+' '+scale(book)+levels+' -strip temp/${page.number}.pnm');
  cmd.exec('${settings.path_opj} -i temp/${page.number}.pnm -q ${settings.jp2_quality} -o temp/${page.number}.jp2');
}
function bookexist_pdfnocrop() {
  return bookexist_pdf();
}
function bookexecute_pdfnocrop() {
  return bookexecute_pdf();
}

function exist_pdforig() {
	return true;
}
function bookexist_pdforig() {
	return bookexist_pdf();
}
function bookexecute_pdforig() {
	var pdf = cmd.pdf2('../'+book.name+'.pdf');
	for each (p in book.pages) {
		pdf.addPage(book.book, p);
	}
	pdf.close();
}
