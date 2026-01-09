// App global JS
// Initialize DataTables on dashboard if present
(function(){
  function initDashboardTable(){
    var el = document.getElementById('recent-transactions');
    if(!el || typeof $ === 'undefined' || !$.fn || !$.fn.DataTable){
      return;
    }
    if ($.fn.dataTable.isDataTable(el)) {
      return;
    }
    $(el).DataTable({
      paging: true,
      searching: true,
      info: false,
      lengthChange: false,
      pageLength: 5,
      order: [[0, 'desc']],
      autoWidth: false,
      columnDefs: [
        { targets: 4, className: 'right' }
      ],
      language: {
        search: 'Filter:',
        paginate: { previous: 'Prev', next: 'Next' }
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDashboardTable);
  } else {
    initDashboardTable();
  }
})();
