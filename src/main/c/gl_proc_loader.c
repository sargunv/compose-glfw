#define _GNU_SOURCE

#include <dlfcn.h>
#include <stddef.h>

typedef void *(*get_proc_address_fn)(const char *);

void *compose_glfw_get_proc(void *ctx, const char *name) {
  (void)ctx;

  static void *egl_library = NULL;
  static void *gl_library = NULL;
  static get_proc_address_fn egl_get_proc_address = NULL;

  if (egl_library == NULL) {
    egl_library = dlopen("libEGL.so.1", RTLD_LAZY | RTLD_LOCAL);
    if (egl_library != NULL) {
      egl_get_proc_address = (get_proc_address_fn)dlsym(egl_library, "eglGetProcAddress");
    }
  }

  if (egl_get_proc_address != NULL) {
    void *address = egl_get_proc_address(name);
    if (address != NULL) {
      return address;
    }
  }

  if (gl_library == NULL) {
    gl_library = dlopen("libGL.so.1", RTLD_LAZY | RTLD_LOCAL);
  }

  if (gl_library != NULL) {
    return dlsym(gl_library, name);
  }

  return NULL;
}
