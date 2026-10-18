function fn() {
  // Entorno: local (app levantada por Maven en acceptance.port) o docker (contenedor en 8080)
  var env = karate.env || 'local';
  var port = karate.properties['acceptance.port'] || '18080';
  var baseUrls = {
    local: 'http://localhost:' + port,
    docker: 'http://localhost:8080'
  };
  var baseUrl = baseUrls[env];
  if (!baseUrl) {
    karate.fail('karate.env desconocido: ' + env);
  }
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);
  karate.log('Karate env =', env, '| baseUrl =', baseUrl);
  return { baseUrl: baseUrl };
}
